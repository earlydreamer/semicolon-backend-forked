package dukku.ai.out;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import dukku.ai.app.service.GeminiEmbeddingService;
import dukku.ai.global.policy.AiSimilarityPolicy;
import dukku.common.shared.ai.dto.HybridSearchResult;
import dukku.common.shared.ai.dto.ProductSearchFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class HybridSearchRepository {

    private final JdbcTemplate jdbcTemplate;
    private final GeminiEmbeddingService embeddingService;

    private static final RowMapper<HybridSearchResult> RESULT_MAPPER = (ResultSet rs, int rowNum) -> new HybridSearchResult(
            UUID.fromString(rs.getString("id")),
            rs.getString("content"),
            rs.getString("metadata"),
            rs.getDouble("vector_score"),
            rs.getDouble("keyword_score"),
            rs.getDouble("rrf_score")
    );

    public List<HybridSearchResult> search(String query, int topK, double vectorThreshold) {
        return search(query, topK, vectorThreshold, ProductSearchFilter.NONE);
    }

    public List<HybridSearchResult> search(String query, int topK, double vectorThreshold,
                                           ProductSearchFilter filter) {
        float[] embedding = embeddingService.embedQuery(query);
        String embeddingStr = toVectorLiteral(embedding);
        ProductSearchFilter effectiveFilter = filter == null ? ProductSearchFilter.NONE : filter;
        int candidateLimit = Math.max(topK, topK * 2);
        FilterSql filterSql = filterSql(effectiveFilter);

        if (!isPgroongaAvailable()) {
            return searchVectorOnly(embeddingStr, topK, vectorThreshold, candidateLimit, filterSql);
        }
        return searchHybrid(query, embeddingStr, topK, vectorThreshold, candidateLimit, filterSql);
    }

    private List<HybridSearchResult> searchHybrid(String query, String embeddingStr, int topK,
                                                  double vectorThreshold, int candidateLimit,
                                                  FilterSql filterSql) {
        int k = AiSimilarityPolicy.RRF_K;
        String sql = """
                WITH keyword AS (
                    SELECT id, content, metadata::text AS metadata,
                           pgroonga_score(tableoid, ctid) AS score,
                           ROW_NUMBER() OVER (ORDER BY pgroonga_score(tableoid, ctid) DESC) AS rank
                    FROM product_search
                    WHERE content &@~ ?%s
                    LIMIT ?
                ),
                semantic AS (
                    SELECT id, content, metadata::text AS metadata,
                           1 - (embedding <=> ?::vector) AS score,
                           ROW_NUMBER() OVER (ORDER BY embedding <=> ?::vector) AS rank
                    FROM product_search
                    WHERE embedding IS NOT NULL
                      AND embedding_profile = ?
                      AND 1 - (embedding <=> ?::vector) >= ?%s
                    LIMIT ?
                )
                SELECT COALESCE(k.id, s.id) AS id,
                       COALESCE(k.content, s.content) AS content,
                       COALESCE(k.metadata, s.metadata) AS metadata,
                       COALESCE(s.score, 0) AS vector_score,
                       COALESCE(k.score, 0) AS keyword_score,
                       COALESCE(1.0 / (? + k.rank), 0) + COALESCE(1.0 / (? + s.rank), 0) AS rrf_score
                FROM keyword k
                FULL OUTER JOIN semantic s ON k.id = s.id
                ORDER BY rrf_score DESC
                LIMIT ?
                """.formatted(filterSql.clause(), filterSql.clause());

        List<Object> params = new ArrayList<>();
        params.add(toKeywordQuery(query));
        params.addAll(filterSql.parameters());
        params.add(candidateLimit);
        params.add(embeddingStr);
        params.add(embeddingStr);
        params.add(embeddingService.profile());
        params.add(embeddingStr);
        params.add(vectorThreshold);
        params.addAll(filterSql.parameters());
        params.add(candidateLimit);
        params.add(k);
        params.add(k);
        params.add(topK);
        return jdbcTemplate.query(sql, RESULT_MAPPER, params.toArray());
    }

    private List<HybridSearchResult> searchVectorOnly(String embeddingStr, int topK,
                                                       double vectorThreshold, int candidateLimit,
                                                       FilterSql filterSql) {
        String sql = """
                WITH semantic AS (
                    SELECT id, content, metadata::text AS metadata,
                           1 - (embedding <=> ?::vector) AS vector_score,
                           ROW_NUMBER() OVER (ORDER BY embedding <=> ?::vector) AS rank
                    FROM product_search
                    WHERE embedding IS NOT NULL
                      AND embedding_profile = ?
                      AND 1 - (embedding <=> ?::vector) >= ?%s
                    LIMIT ?
                )
                SELECT id, content, metadata, vector_score,
                       0::double precision AS keyword_score,
                       1.0 / (? + rank) AS rrf_score
                FROM semantic
                ORDER BY rrf_score DESC
                LIMIT ?
                """.formatted(filterSql.clause());

        List<Object> params = new ArrayList<>();
        params.add(embeddingStr);
        params.add(embeddingStr);
        params.add(embeddingService.profile());
        params.add(embeddingStr);
        params.add(vectorThreshold);
        params.addAll(filterSql.parameters());
        params.add(candidateLimit);
        params.add(AiSimilarityPolicy.RRF_K);
        params.add(topK);
        return jdbcTemplate.query(sql, RESULT_MAPPER, params.toArray());
    }

    public void upsert(UUID productUuid, String content, String metadata, float[] embedding) {
        String embeddingStr = toVectorLiteral(embedding);
        String sql = """
                INSERT INTO product_search (id, content, metadata, embedding, embedding_profile)
                VALUES (?, ?, ?::jsonb, ?::vector, ?)
                ON CONFLICT (id) DO UPDATE
                    SET content = EXCLUDED.content,
                        metadata = EXCLUDED.metadata,
                        embedding = EXCLUDED.embedding,
                        embedding_profile = EXCLUDED.embedding_profile
                """;

        jdbcTemplate.update(sql, productUuid, content, metadata, embeddingStr, embeddingService.profile());
    }

    public void delete(UUID productUuid) {
        jdbcTemplate.update("DELETE FROM product_search WHERE id = ?", productUuid);
    }

    public float[] embedDocument(String text) {
        return embeddingService.embedDocument(text);
    }

    private boolean isPgroongaAvailable() {
        try {
            Boolean installed = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pgroonga')", Boolean.class);
            return Boolean.TRUE.equals(installed);
        } catch (DataAccessException e) {
            log.debug("PGroonga extension lookup unavailable; using vector-only search");
            return false;
        }
    }

    private static FilterSql filterSql(ProductSearchFilter filter) {
        StringBuilder clause = new StringBuilder();
        List<Object> parameters = new ArrayList<>();

        if (filter.hasMinPrice()) {
            clause.append(" AND (metadata->>'price')::bigint >= ?");
            parameters.add(filter.minPrice());
        }
        if (filter.hasMaxPrice()) {
            clause.append(" AND (metadata->>'price')::bigint <= ?");
            parameters.add(filter.maxPrice());
        }
        return new FilterSql(clause.toString(), List.copyOf(parameters));
    }

    private static String toKeywordQuery(String query) {
        String orQuery = Arrays.stream(query.split("\\s+"))
                .filter(word -> word.length() >= 2)
                .collect(Collectors.joining(" OR "));
        return orQuery.isEmpty() ? query : orQuery;
    }

    private static String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private record FilterSql(String clause, List<Object> parameters) {
    }
}
