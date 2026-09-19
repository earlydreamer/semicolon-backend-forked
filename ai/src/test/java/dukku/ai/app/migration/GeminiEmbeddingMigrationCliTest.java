package dukku.ai.app.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import dukku.ai.app.service.GeminiEmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class GeminiEmbeddingMigrationCliTest {

    private static final String MODEL = "gemini-embedding-001";
    private static final String PROFILE = GeminiEmbeddingService.profileFor(MODEL, 1536);
    private static final UUID PRODUCT_ONE = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID PRODUCT_TWO = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID PRODUCT_THREE = UUID.fromString("00000000-0000-0000-0000-000000000103");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @BeforeEach
    void recreateTables() throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS public.product_search");
            statement.execute("DROP TABLE IF EXISTS public.ai_user_memory");
            statement.execute("CREATE EXTENSION IF NOT EXISTS vector");
            statement.execute("""
                    CREATE TABLE public.ai_user_memory (
                        id integer PRIMARY KEY,
                        content text NOT NULL,
                        embedding vector(1536)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE public.product_search (
                        id uuid PRIMARY KEY,
                        content text NOT NULL,
                        embedding vector(1536)
                    )
                    """);
        }
    }

    @Test
    void profileSqlIsNullableIdempotentAndPreservesExistingRows() throws Exception {
        insertMemory(40, "preserve-me", oneHotLiteral(7), null);

        applyProfileSql();
        applyProfileSql();

        assertColumnNullable("ai_user_memory");
        assertColumnNullable("product_search");
        MemoryState state = readMemory(40);
        assertEquals(40, state.id());
        assertEquals("preserve-me", state.content());
        assertNull(state.profile());
        assertVectorComponent(state.embeddingText(), 7);
    }

    @Test
    void dryRunNeedsNoKeyOrEmbeddingProviderAndDoesNotChangeSevenNullRows() throws Exception {
        applyProfileSql();
        seedSevenNullRows();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = execute(
                new String[] {"--dry-run", "--batch-size", "2"},
                Map.of("EMBEDDING_MODEL", MODEL),
                output,
                (apiKey, model) -> {
                    throw new AssertionError("dry-run must not create an embedding client");
                });

        assertEquals(0, exitCode);
        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("mode=dry-run"));
        assertTrue(text.contains("candidates=7"));
        assertTrue(text.contains("updated=0"));
        assertTrue(text.contains("failures=0"));
        assertEquals(4, count("ai_user_memory"));
        assertEquals(3, count("product_search"));
        assertEquals(7, countNullEmbeddings());
        assertEquals(0, countProfiles());
    }

    @Test
    void failedApiRowIsPreservedThenRetryCompletesAndFollowingRunSkipsAll() throws Exception {
        applyProfileSql();
        seedSevenNullRows();
        ByteArrayOutputStream firstOutput = new ByteArrayOutputStream();

        int failedExitCode = execute(
                new String[] {"--apply", "--batch-size", "2"},
                applyEnvironment(),
                firstOutput,
                (apiKey, model) -> content -> {
                    if (content.equals("memory-2")) {
                        throw new IllegalStateException("private-sentinel-" + content);
                    }
                    return oneHotVector(content.hashCode());
                });

        assertEquals(1, failedExitCode);
        String firstReport = firstOutput.toString(StandardCharsets.UTF_8);
        assertTrue(firstReport.contains("failures=1"));
        assertTrue(firstReport.contains("IllegalStateException:1"));
        assertFalse(firstReport.contains("private-sentinel"));
        assertFalse(firstReport.contains("memory-2"));
        MemoryState failedRow = readMemory(2);
        assertEquals("memory-2", failedRow.content());
        assertNull(failedRow.embeddingText());
        assertNull(failedRow.profile());
        assertEquals(1, countNullEmbeddings());

        ByteArrayOutputStream retryOutput = new ByteArrayOutputStream();
        int retryExitCode = execute(
                new String[] {"--apply", "--batch-size", "3"},
                applyEnvironment(),
                retryOutput,
                (apiKey, model) -> content -> oneHotVector(Math.floorMod(content.hashCode(), 1536)));

        assertEquals(0, retryExitCode);
        String retryReport = retryOutput.toString(StandardCharsets.UTF_8);
        assertTrue(retryReport.contains("candidates=1"));
        assertTrue(retryReport.contains("updated=1"));
        assertTrue(retryReport.contains("skipped=6"));
        assertEquals(0, countNullEmbeddings());
        assertContentAndIdentityPreserved();
        assertAllRowsHaveProfileAndUnitVector();

        ByteArrayOutputStream skipOutput = new ByteArrayOutputStream();
        int skipExitCode = execute(
                new String[] {"--apply"},
                applyEnvironment(),
                skipOutput,
                (apiKey, model) -> content -> {
                    throw new AssertionError("valid same-profile rows must be skipped");
                });
        assertEquals(0, skipExitCode);
        assertTrue(skipOutput.toString(StandardCharsets.UTF_8).contains("candidates=0"));
        assertTrue(skipOutput.toString(StandardCharsets.UTF_8).contains("skipped=7"));
    }

    @Test
    void invalidVectorWithMatchingProfileIsRegenerated() throws Exception {
        applyProfileSql();
        insertMemory(55, "invalid-current-profile", zeroVectorLiteral(), PROFILE);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = execute(
                new String[] {"--apply"},
                applyEnvironment(),
                output,
                (apiKey, model) -> content -> oneHotVector(15));

        assertEquals(0, exitCode);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("candidates=1"));
        assertEquals(PROFILE, readMemory(55).profile());
        assertVectorComponent(readMemory(55).embeddingText(), 15);
    }

    @Test
    void maxRowsBoundsAttemptsAcrossBothTables() throws Exception {
        applyProfileSql();
        seedSevenNullRows();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = execute(
                new String[] {"--apply", "--batch-size", "2", "--max-rows", "3"},
                applyEnvironment(),
                output,
                (apiKey, model) -> content -> oneHotVector(Math.floorMod(content.hashCode(), 1536)));

        assertEquals(0, exitCode);
        String report = output.toString(StandardCharsets.UTF_8);
        assertTrue(report.contains("candidates=3"));
        assertTrue(report.contains("updated=3"));
        assertTrue(report.contains("limit_reached=true"));
        assertEquals(4, countNullEmbeddings());
        assertNotNull(readMemory(1).embeddingText());
        assertNotNull(readMemory(2).embeddingText());
        assertNotNull(readMemory(3).embeddingText());
        assertNull(readMemory(4).embeddingText());
        assertEquals(3, countNullEmbeddings("product_search"));
    }

    @Test
    void concurrentContentAndEmbeddingChangeIsNotOverwritten() throws Exception {
        applyProfileSql();
        insertMemory(77, "version-1", oneHotLiteral(0), "previous-model:1536:retrieval-document:normalization-v1");
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = execute(
                new String[] {"--apply", "--batch-size", "1"},
                applyEnvironment(),
                output,
                (apiKey, model) -> content -> {
                    try (Connection concurrent = openConnection(); PreparedStatement statement = concurrent.prepareStatement("""
                            UPDATE public.ai_user_memory
                            SET content = ?, embedding = CAST(? AS vector), embedding_profile = ?
                            WHERE id = ?
                            """)) {
                        statement.setString(1, "version-2");
                        statement.setString(2, oneHotLiteral(1));
                        statement.setString(3, PROFILE);
                        statement.setInt(4, 77);
                        assertEquals(1, statement.executeUpdate());
                    } catch (SQLException error) {
                        throw new IllegalStateException(error);
                    }
                    return oneHotVector(2);
                });

        assertEquals(1, exitCode);
        String report = output.toString(StandardCharsets.UTF_8);
        assertTrue(report.contains("conflicts=1"));
        assertTrue(report.contains("updated=0"));
        MemoryState state = readMemory(77);
        assertEquals("version-2", state.content());
        assertEquals(PROFILE, state.profile());
        assertVectorComponent(state.embeddingText(), 1);
    }

    @Test
    void missingRequiredTableFailsPreflightBeforeCreatingEmbeddingProvider() throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE public.product_search");
        }
        applyProfileSql();
        insertMemory(91, "must-remain-null", null, null);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = execute(
                new String[] {"--apply"},
                applyEnvironment(),
                output,
                (apiKey, model) -> {
                    throw new AssertionError("schema preflight must happen before creating an embedding client");
                });

        assertEquals(2, exitCode);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Required tables or 1536-dimensional profile schema are missing"));
        assertNull(readMemory(91).embeddingText());
        assertNull(readMemory(91).profile());
    }

    @Test
    void wrongVectorDimensionFailsPreflightBeforeEitherTableIsChanged() throws Exception {
        applyProfileSql();
        insertMemory(92, "must-remain-null", null, null);
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE public.product_search ALTER COLUMN embedding TYPE vector(1535)");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = execute(
                new String[] {"--apply"},
                applyEnvironment(),
                output,
                (apiKey, model) -> {
                    throw new AssertionError("dimension preflight must happen before creating an embedding client");
                });

        assertEquals(2, exitCode);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("1536-dimensional profile schema"));
        assertNull(readMemory(92).embeddingText());
        assertNull(readMemory(92).profile());
    }

    private static int execute(
            String[] args,
            Map<String, String> environment,
            ByteArrayOutputStream output,
            GeminiEmbeddingMigrationCli.EmbeddingProviderFactory embeddingFactory) throws SQLException {
        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            return GeminiEmbeddingMigrationCli.execute(
                    args,
                    environment,
                    stream,
                    ignored -> openConnection(),
                    embeddingFactory);
        }
    }

    private static Map<String, String> applyEnvironment() {
        return Map.of("EMBEDDING_MODEL", MODEL, "GEMINI_API_KEY", "test-key");
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void applyProfileSql() throws Exception {
        try (InputStream input = GeminiEmbeddingMigrationCliTest.class.getClassLoader()
                .getResourceAsStream("sql/manual/20260919_gemini_embedding_profile.sql")) {
            assertNotNull(input, "profile SQL must be packaged as a resource");
            String script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
                for (String part : script.split(";")) {
                    String sql = part.strip();
                    if (!sql.isEmpty()) {
                        statement.execute(sql);
                    }
                }
            }
        }
    }

    private static void seedSevenNullRows() throws SQLException {
        insertMemory(1, "memory-1", null, null);
        insertMemory(2, "memory-2", null, null);
        insertMemory(3, "memory-3", null, null);
        insertMemory(4, "memory-4", null, null);
        insertProduct(PRODUCT_ONE, "product-1", null, null);
        insertProduct(PRODUCT_TWO, "product-2", null, null);
        insertProduct(PRODUCT_THREE, "product-3", null, null);
    }

    private static void insertMemory(int id, String content, String vector, String profile) throws SQLException {
        String sql = """
                INSERT INTO public.ai_user_memory (id, content, embedding%s)
                VALUES (?, ?, CAST(? AS vector)%s)
                """.formatted(profileColumn(), profileValue());
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, content);
            setNullableString(statement, 3, vector);
            if (profile != null || hasProfileColumn("ai_user_memory")) {
                setNullableString(statement, 4, profile);
            }
            statement.executeUpdate();
        }
    }

    private static void insertProduct(UUID id, String content, String vector, String profile) throws SQLException {
        String sql = """
                INSERT INTO public.product_search (id, content, embedding%s)
                VALUES (?, ?, CAST(? AS vector)%s)
                """.formatted(profileColumn(), profileValue());
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            statement.setString(2, content);
            setNullableString(statement, 3, vector);
            if (profile != null || hasProfileColumn("product_search")) {
                setNullableString(statement, 4, profile);
            }
            statement.executeUpdate();
        }
    }

    private static String profileColumn() throws SQLException {
        return hasProfileColumn("ai_user_memory") ? ", embedding_profile" : "";
    }

    private static String profileValue() throws SQLException {
        return hasProfileColumn("ai_user_memory") ? ", ?" : "";
    }

    private static boolean hasProfileColumn(String table) throws SQLException {
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = 'embedding_profile'
                """)) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) == 1;
            }
        }
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private static void assertColumnNullable(String table) throws SQLException {
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement("""
                SELECT is_nullable, character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = 'embedding_profile'
                """)) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals("YES", result.getString(1));
                assertEquals(160, result.getInt(2));
            }
        }
    }

    private static void assertContentAndIdentityPreserved() throws SQLException {
        assertEquals("memory-1", readMemory(1).content());
        assertEquals("memory-2", readMemory(2).content());
        assertEquals("memory-3", readMemory(3).content());
        assertEquals("memory-4", readMemory(4).content());
        assertEquals("product-1", readProduct(PRODUCT_ONE).content());
        assertEquals("product-2", readProduct(PRODUCT_TWO).content());
        assertEquals("product-3", readProduct(PRODUCT_THREE).content());
        assertEquals(PRODUCT_ONE, readProduct(PRODUCT_ONE).id());
        assertEquals(PRODUCT_TWO, readProduct(PRODUCT_TWO).id());
        assertEquals(PRODUCT_THREE, readProduct(PRODUCT_THREE).id());
    }

    private static void assertAllRowsHaveProfileAndUnitVector() throws SQLException {
        for (int id = 1; id <= 4; id++) {
            MemoryState row = readMemory(id);
            assertEquals(PROFILE, row.profile());
            assertUnitVector(row.embeddingText());
        }
        for (UUID id : new UUID[] {PRODUCT_ONE, PRODUCT_TWO, PRODUCT_THREE}) {
            MemoryState row = readProduct(id);
            assertEquals(PROFILE, row.profile());
            assertUnitVector(row.embeddingText());
        }
    }

    private static void assertUnitVector(String text) {
        float[] vector = parseVector(text);
        assertEquals(1536, vector.length);
        double norm = 0.0;
        for (float value : vector) {
            assertTrue(Float.isFinite(value));
            norm += (double) value * value;
        }
        assertEquals(1.0, Math.sqrt(norm), 1.0e-5);
    }

    private static void assertVectorComponent(String text, int index) {
        float[] vector = parseVector(text);
        assertEquals(1536, vector.length);
        assertEquals(1.0f, vector[index]);
        for (int i = 0; i < vector.length; i++) {
            if (i != index) {
                assertEquals(0.0f, vector[i]);
            }
        }
    }

    private static float[] parseVector(String text) {
        assertNotNull(text);
        String[] parts = text.substring(1, text.length() - 1).split(",", -1);
        float[] values = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Float.parseFloat(parts[i].trim());
        }
        return values;
    }

    private static long count(String table) throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM public." + table)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static long countNullEmbeddings() throws SQLException {
        return countNullEmbeddings("ai_user_memory") + countNullEmbeddings("product_search");
    }

    private static long countNullEmbeddings(String table) throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT COUNT(*) FROM public." + table + " WHERE embedding IS NULL")) {
            result.next();
            return result.getLong(1);
        }
    }

    private static long countProfiles() throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT
                            (SELECT COUNT(*) FROM public.ai_user_memory WHERE embedding_profile IS NOT NULL)
                          + (SELECT COUNT(*) FROM public.product_search WHERE embedding_profile IS NOT NULL)
                        """)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static MemoryState readMemory(int id) throws SQLException {
        return readRow("ai_user_memory", "id", id);
    }

    private static MemoryState readProduct(UUID id) throws SQLException {
        return readRow("product_search", "id", id);
    }

    private static MemoryState readRow(String table, String idColumn, Object id) throws SQLException {
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT id, content, embedding::text, embedding_profile FROM public." + table + " WHERE " + idColumn + " = ?")) {
            statement.setObject(1, id);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                Object value = result.getObject(1);
                UUID uuid = value instanceof UUID ? (UUID) value : null;
                return new MemoryState(uuid == null ? value : uuid, result.getString(2), result.getString(3), result.getString(4));
            }
        }
    }

    private static String oneHotLiteral(int component) {
        float[] values = oneHotVector(component);
        StringBuilder builder = new StringBuilder(values.length * 2).append('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(Float.toString(values[i]));
        }
        return builder.append(']').toString();
    }

    private static String zeroVectorLiteral() {
        StringBuilder builder = new StringBuilder(4_000).append('[');
        for (int i = 0; i < 1536; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('0');
        }
        return builder.append(']').toString();
    }

    private static float[] oneHotVector(int seed) {
        float[] vector = new float[1536];
        vector[Math.floorMod(seed, vector.length)] = 1.0f;
        return vector;
    }

    private record MemoryState(Object id, String content, String embeddingText, String profile) {
    }
}