package dukku.ai.out;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import dukku.ai.app.service.GeminiEmbeddingService;
import dukku.ai.entity.AiUserMemory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AiUserMemoryRepositoryPostgresTest {

    private static final String PROFILE = GeminiEmbeddingService.profileFor("gemini-embedding-001", 1536);
    private static final UUID USER = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private AiUserMemoryRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @BeforeAll
    static void createActualMemoryTable() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE EXTENSION IF NOT EXISTS vector");
            statement.execute("""
                    CREATE TABLE public.ai_user_memory (
                        id serial PRIMARY KEY,
                        uuid uuid NOT NULL UNIQUE,
                        created_at timestamp without time zone NOT NULL,
                        updated_at timestamp without time zone,
                        user_uuid uuid NOT NULL,
                        memory_type varchar(50) NOT NULL,
                        sub_type varchar(50) NOT NULL,
                        content text NOT NULL,
                        embedding vector(1536),
                        embedding_profile varchar(160),
                        importance_score double precision NOT NULL,
                        access_count integer NOT NULL DEFAULT 0
                    )
                    """);
        }
    }

    @BeforeEach
    void clearRows() {
        jdbcTemplate.update("TRUNCATE TABLE public.ai_user_memory RESTART IDENTITY");
    }

    @Test
    void profileMemoriesRemainReadableWhenTheyHaveNoEmbedding() {
        insert(USER, "PROFILE", "사용자는 간결한 답변을 선호함", null, null);

        List<AiUserMemory> profiles = repository.findTopByUserIdAndMemoryType(USER, "PROFILE", 10);

        assertEquals(1, profiles.size());
        assertEquals("사용자는 간결한 답변을 선호함", profiles.getFirst().getContent());
        assertNull(profiles.getFirst().getEmbedding());
        assertNull(profiles.getFirst().getEmbeddingProfile());
    }

    @Test
    void similarityAndDuplicateQueriesOnlyReturnCurrentProfileVectorsFromCanonicalTable() {
        Integer currentId = insert(USER, "PREFERENCE", "현재 프로필 벡터", oneHotLiteral(0), PROFILE);
        insert(USER, "PREFERENCE", "이전 프로필 벡터", oneHotLiteral(0),
                "previous-model:1536:retrieval-document:normalization-v1");
        insert(USER, "PREFERENCE", "프로필이 없는 벡터", oneHotLiteral(0), null);
        insert(USER, "PROFILE", "임베딩 없는 프로필", null, null);
        insert(OTHER_USER, "PREFERENCE", "다른 사용자의 벡터", oneHotLiteral(0), PROFILE);

        String queryVector = oneHotLiteral(0);
        List<AiUserMemory> similar = repository.findSimilarMemories(USER, queryVector, PROFILE, 0.9, 10);
        List<AiUserMemory> duplicates = repository.findDuplicateMemory(USER, queryVector, PROFILE, 0.9);

        assertEquals(List.of(currentId), similar.stream().map(AiUserMemory::getId).toList());
        assertEquals(List.of(currentId), duplicates.stream().map(AiUserMemory::getId).toList());
        assertEquals("현재 프로필 벡터", similar.getFirst().getContent());
        assertEquals(PROFILE, similar.getFirst().getEmbeddingProfile());
    }

    private Integer insert(UUID userUuid, String memoryType, String content, String vector, String profile) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO public.ai_user_memory (
                    uuid, created_at, updated_at, user_uuid, memory_type, sub_type, content,
                    embedding, embedding_profile, importance_score, access_count)
                VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, 'GENERAL', ?, CAST(? AS vector), ?, 0.8, 0)
                RETURNING id
                """, Integer.class, UUID.randomUUID(), userUuid, memoryType, content, vector, profile);
    }

    private static String oneHotLiteral(int component) {
        StringBuilder value = new StringBuilder(1536 * 2).append('[');
        for (int i = 0; i < 1536; i++) {
            if (i > 0) {
                value.append(',');
            }
            value.append(i == component ? "1.0" : "0.0");
        }
        return value.append(']').toString();
    }
}