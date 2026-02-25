package dukku.ai.out;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import dukku.common.shared.ai.dto.HybridSearchResult;
import dukku.common.shared.ai.dto.ProductSearchFilter;
import dukku.ai.global.policy.AiSimilarityPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class HybridSearchRepository {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;

    private static final RowMapper<HybridSearchResult> RESULT_MAPPER = (ResultSet rs, int rowNum) -> new HybridSearchResult(
            UUID.fromString(rs.getString("id")),
            rs.getString("content"),
            rs.getString("metadata"),
            rs.getDouble("vector_score"),
            rs.getDouble("keyword_score"),
            rs.getDouble("rrf_score")
    );

    /**
     * RRF 기반 하이브리드 검색 (필터 없음)
     */
    public List<HybridSearchResult> search(String query, int topK, double vectorThreshold) {
        return search(query, topK, vectorThreshold, ProductSearchFilter.NONE);
    }

    /**
     * RRF 기반 하이브리드 검색 (metadata 필터 지원)
     * - keyword_rank: PGroonga 전문 검색 결과 순위
     * - vector_rank: pgvector 코사인 유사도 순위
     * - RRF score = 1/(k + keyword_rank) + 1/(k + vector_rank)
     * - metadata 필터: price 범위, saleStatus 조건
     */
    public List<HybridSearchResult> search(String query, int topK, double vectorThreshold,
                                           ProductSearchFilter filter) {
        float[] embedding = embeddingModel.embed(query);
        String embeddingStr = toVectorLiteral(embedding);
        int k = AiSimilarityPolicy.RRF_K;
        int candidateLimit = topK * 2;

        // metadata 필터 조건 동적 생성
        StringBuilder metadataFilter = new StringBuilder();
        List<Object> filterParams = new ArrayList<>();

        if (filter.hasMinPrice()) {
            metadataFilter.append(" AND (metadata->>'price')::bigint >= ?");
            filterParams.add(filter.minPrice());
        }
        if (filter.hasMaxPrice()) {
            metadataFilter.append(" AND (metadata->>'price')::bigint <= ?");
            filterParams.add(filter.maxPrice());
        }

        String filterClause = metadataFilter.toString();

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
                    WHERE 1 - (embedding <=> ?::vector) >= ?%s
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
                """.formatted(filterClause, filterClause);

        // 파라미터 조립: keyword CTE params + semantic CTE params + RRF params
        String keywordQuery = toKeywordQuery(query);
        List<Object> params = new ArrayList<>();
        params.add(keywordQuery);
        params.addAll(filterParams);
        params.add(candidateLimit);
        params.add(embeddingStr);
        params.add(embeddingStr);
        params.add(embeddingStr);
        params.add(vectorThreshold);
        params.addAll(filterParams);
        params.add(candidateLimit);
        params.add(k);
        params.add(k);
        params.add(topK);

        return jdbcTemplate.query(sql, RESULT_MAPPER, params.toArray());
    }

    public void upsert(UUID productUuid, String content, String metadata, float[] embedding) {
        String embeddingStr = toVectorLiteral(embedding);

        String sql = """
                INSERT INTO product_search (id, content, metadata, embedding)
                VALUES (?, ?, ?::jsonb, ?::vector)
                ON CONFLICT (id) DO UPDATE
                    SET content = EXCLUDED.content,
                        metadata = EXCLUDED.metadata,
                        embedding = EXCLUDED.embedding
                """;

        jdbcTemplate.update(sql, productUuid, content, metadata, embeddingStr);
    }

    public void delete(UUID productUuid) {
        jdbcTemplate.update("DELETE FROM product_search WHERE id = ?", productUuid);
    }

    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    /**
     * 자연어 쿼리를 PGroonga OR 검색 쿼리로 변환
     * "가성비 좋은 캠핑 의자 추천해줘" → "캠핑 OR 의자 OR 추천해줘"
     * 1글자 단어는 노이즈이므로 제거
     */
    private static String toKeywordQuery(String query) {
        String orQuery = Arrays.stream(query.split("\\s+"))
                .filter(word -> word.length() >= 2)
                .collect(Collectors.joining(" OR "));
        return orQuery.isEmpty() ? query : orQuery;
    }

    private static String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
