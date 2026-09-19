package dukku.ai.out;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import dukku.ai.app.service.GeminiEmbeddingService;
import dukku.common.shared.ai.dto.HybridSearchResult;
import dukku.common.shared.ai.dto.ProductSearchFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class HybridSearchRepositoryPostgresTest {

    private static final String PROFILE = GeminiEmbeddingService.profileFor("gemini-embedding-001", 1536);
    private static final UUID IN_RANGE = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TOO_EXPENSIVE = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID TOO_CHEAP = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID STALE_PROFILE = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final UUID UNKNOWN_PROFILE = UUID.fromString("10000000-0000-0000-0000-000000000005");
    private static final UUID BELOW_THRESHOLD = UUID.fromString("10000000-0000-0000-0000-000000000006");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void preparePostgresWithoutPgroonga() throws SQLException {
        jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS public.product_search");
            statement.execute("CREATE EXTENSION IF NOT EXISTS vector");
            statement.execute("""
                    CREATE TABLE public.product_search (
                        id uuid PRIMARY KEY,
                        content text NOT NULL,
                        metadata jsonb,
                        embedding vector(1536),
                        embedding_profile varchar(160)
                    )
                    """);
        }
    }

    @Test
    void vectorOnlySearchAppliesPriceFilterAndRejectsStaleOrUnknownProfiles() throws Exception {
        assertFalse(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pgroonga')", Boolean.class));

        insert(IN_RANGE, "current in range", 100, oneHotLiteral(0), PROFILE);
        insert(TOO_EXPENSIVE, "current too expensive", 1_000, oneHotLiteral(0), PROFILE);
        insert(TOO_CHEAP, "current too cheap", 40, oneHotLiteral(0), PROFILE);
        insert(STALE_PROFILE, "stale embedding", 100, oneHotLiteral(0),
                "previous-model:1536:retrieval-document:normalization-v1");
        insert(UNKNOWN_PROFILE, "unknown embedding", 100, oneHotLiteral(0), null);
        insert(BELOW_THRESHOLD, "below threshold", 100, oneHotLiteral(1), PROFILE);

        GeminiEmbeddingService embeddingService = mock(GeminiEmbeddingService.class);
        when(embeddingService.embedQuery("semantic-query")).thenReturn(oneHotVector(0));
        when(embeddingService.profile()).thenReturn(PROFILE);
        HybridSearchRepository repository = new HybridSearchRepository(jdbcTemplate, embeddingService);

        List<HybridSearchResult> results = repository.search(
                "semantic-query", 10, 0.9, new ProductSearchFilter(50L, 500L));

        assertEquals(List.of(IN_RANGE), results.stream().map(HybridSearchResult::id).toList());
        HybridSearchResult result = results.getFirst();
        assertEquals("current in range", result.content());
        assertEquals(1.0, result.vectorScore(), 1.0e-6);
        assertEquals(0.0, result.keywordScore());
        assertTrue(result.rrfScore() > 0.0);
    }

    private void insert(UUID id, String content, long price, String vector, String profile) {
        String metadata = "{" + (char) 34 + "price" + (char) 34 + ":" + price + "}";
        jdbcTemplate.update("""
                INSERT INTO public.product_search (id, content, metadata, embedding, embedding_profile)
                VALUES (?, ?, CAST(? AS jsonb), CAST(? AS vector), ?)
                """, id, content, metadata, vector, profile);
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static float[] oneHotVector(int component) {
        float[] vector = new float[1536];
        vector[component] = 1.0f;
        return vector;
    }

    private static String oneHotLiteral(int component) {
        float[] vector = oneHotVector(component);
        StringBuilder value = new StringBuilder(vector.length * 2).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                value.append(',');
            }
            value.append(Float.toString(vector[i]));
        }
        return value.append(']').toString();
    }
}