package dukku.ai.out;

import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import dukku.ai.app.dto.HybridSearchResult;
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
            rs.getDouble("rrf_score")
    );

    /**
     * RRF 기반 하이브리드 검색
     * - keyword_rank: PGroonga 전문 검색 결과 순위
     * - vector_rank: pgvector 코사인 유사도 순위
     * - RRF score = 1/(k + keyword_rank) + 1/(k + vector_rank)
     */
    public List<HybridSearchResult> search(String query, int topK, double vectorThreshold) {
        float[] embedding = embeddingModel.embed(query);
        String embeddingStr = toVectorLiteral(embedding);
        int k = AiSimilarityPolicy.RRF_K;
        int candidateLimit = topK * 2;

        String sql = """
                WITH keyword AS (
                    SELECT id, content, metadata::text AS metadata,
                           ROW_NUMBER() OVER (ORDER BY pgroonga_score(tableoid, ctid) DESC) AS rank
                    FROM product_search
                    WHERE content &@~ ?
                    LIMIT ?
                ),
                semantic AS (
                    SELECT id, content, metadata::text AS metadata,
                           1 - (embedding <=> ?::vector) AS score,
                           ROW_NUMBER() OVER (ORDER BY embedding <=> ?::vector) AS rank
                    FROM product_search
                    WHERE 1 - (embedding <=> ?::vector) >= ?
                    LIMIT ?
                )
                SELECT COALESCE(k.id, s.id) AS id,
                       COALESCE(k.content, s.content) AS content,
                       COALESCE(k.metadata, s.metadata) AS metadata,
                       COALESCE(s.score, 0) AS vector_score,
                       COALESCE(1.0 / (? + k.rank), 0) + COALESCE(1.0 / (? + s.rank), 0) AS rrf_score
                FROM keyword k
                FULL OUTER JOIN semantic s ON k.id = s.id
                ORDER BY rrf_score DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, RESULT_MAPPER,
                query, candidateLimit,
                embeddingStr, embeddingStr, embeddingStr, vectorThreshold, candidateLimit,
                k, k,
                topK);
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
