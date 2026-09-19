package dukku.ai.app.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;

/** PostgreSQL keyset backfill for retrieval-document embeddings. */
final class GeminiEmbeddingBackfill {

    private static final double NORMALIZED_NORM_TOLERANCE = 1.0e-3;

    private static final List<TableSpec> TABLES = List.of(
            new TableSpec("ai_user_memory", "integer"),
            new TableSpec("product_search", "uuid"));

    private GeminiEmbeddingBackfill() {
    }

    static BackfillSummary run(
            Connection connection,
            boolean apply,
            int batchSize,
            long maxRows,
            String profile,
            GeminiEmbeddingMigrationCli.DocumentEmbedder embedder) throws SQLException {
        if (!connection.getAutoCommit()) {
            throw new SQLException("Migration connection must use auto-commit to keep API calls outside transactions");
        }
        preflight(connection);
        if (batchSize < 1 || maxRows < 1) {
            throw new IllegalArgumentException("batchSize and maxRows must be positive");
        }
        if (apply && embedder == null) {
            throw new IllegalArgumentException("An embedding provider is required in apply mode");
        }

        Map<String, TableBackfillSummary> tableSummaries = new LinkedHashMap<>();
        long remainingCandidates = maxRows;
        boolean limitReached = false;

        for (TableSpec table : TABLES) {
            if (!tableExists(connection, table)) {
                throw new MigrationSchemaException();
            }
            ensureExpectedColumns(connection, table);

            if (remainingCandidates == 0) {
                tableSummaries.put(table.name(), TableBackfillSummary.empty(table.name()));
                limitReached = true;
                continue;
            }

            TableBackfillSummary summary = backfillTable(
                    connection, table, apply, batchSize, remainingCandidates, profile, embedder);
            tableSummaries.put(table.name(), summary);
            remainingCandidates -= summary.candidates();
            if (remainingCandidates == 0 && maxRows != Long.MAX_VALUE) {
                limitReached = true;
            }
        }

        return new BackfillSummary(apply, profile, tableSummaries, limitReached);
    }

    static void preflight(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) {
            throw new SQLException("Migration connection must use auto-commit to keep API calls outside transactions");
        }
        for (TableSpec table : TABLES) {
            if (!tableExists(connection, table)) {
                throw new MigrationSchemaException();
            }
            ensureExpectedColumns(connection, table);
        }
    }

    private static TableBackfillSummary backfillTable(
            Connection connection,
            TableSpec table,
            boolean apply,
            int batchSize,
            long maxCandidates,
            String profile,
            GeminiEmbeddingMigrationCli.DocumentEmbedder embedder) throws SQLException {
        long scanned = 0;
        long candidates = 0;
        long updated = 0;
        long skipped = 0;
        long conflicts = 0;
        long failures = 0;
        Map<String, Long> failureTypes = new LinkedHashMap<>();
        Object cursor = null;
        boolean stop = false;

        while (!stop) {
            List<MemoryRow> page = readPage(connection, table, cursor, batchSize);
            if (page.isEmpty()) {
                break;
            }

            for (MemoryRow row : page) {
                scanned++;
                cursor = row.id();

                if (profile.equals(row.profile()) && isValidNormalizedVector(row.embeddingText())) {
                    skipped++;
                    continue;
                }

                candidates++;
                if (apply) {
                    try {
                        float[] vector = embedder.embedDocument(row.content());
                        if (!isValidNormalizedVector(vector)) {
                            throw new InvalidEmbeddingException();
                        }
                        if (compareAndSet(connection, table, row, profile, vectorLiteral(vector))) {
                            updated++;
                        } else {
                            conflicts++;
                        }
                    } catch (Exception error) {
                        failures++;
                        failureTypes.merge(failureType(error), 1L, Long::sum);
                    }
                }

                if (candidates >= maxCandidates) {
                    stop = true;
                    break;
                }
            }
        }

        return new TableBackfillSummary(
                table.name(), true, scanned, candidates, updated, skipped, conflicts, failures, failureTypes);
    }

    private static boolean tableExists(Connection connection, TableSpec table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT to_regclass(?)")) {
            statement.setString(1, "public." + table.name());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getString(1) != null;
            }
        }
    }

    private static void ensureExpectedColumns(Connection connection, TableSpec table) throws SQLException {
        Map<String, ColumnMetadata> columns = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT column_name, data_type, udt_name, is_nullable, character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name IN ('id', 'content', 'embedding', 'embedding_profile')
                """)) {
            statement.setString(1, table.name());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Number lengthValue = (Number) result.getObject(5);
                    columns.put(result.getString(1), new ColumnMetadata(
                            result.getString(2),
                            result.getString(3),
                            result.getString(4),
                            lengthValue == null ? null : lengthValue.intValue()));
                }
            }
        }

        ColumnMetadata id = columns.get("id");
        ColumnMetadata content = columns.get("content");
        ColumnMetadata embedding = columns.get("embedding");
        ColumnMetadata profile = columns.get("embedding_profile");
        if (id == null || content == null || embedding == null || profile == null
                || !id.dataType().equals(table.expectedIdType())
                || !(content.dataType().equals("text") || content.dataType().equals("character varying"))
                || !embedding.udtName().equals("vector")
                || !profile.dataType().equals("character varying")
                || !"YES".equals(profile.nullable())
                || profile.maximumLength() == null || profile.maximumLength() != 160) {
            throw new MigrationSchemaException();
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT format_type(attribute.atttypid, attribute.atttypmod)
                FROM pg_attribute attribute
                WHERE attribute.attrelid = to_regclass(?)
                  AND attribute.attname = 'embedding'
                  AND NOT attribute.attisdropped
                """)) {
            statement.setString(1, "public." + table.name());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MigrationSchemaException();
                }
                String declaredType = result.getString(1).replace("\"", "").toLowerCase(Locale.ROOT);
                if (!declaredType.endsWith("vector(1536)")) {
                    throw new MigrationSchemaException();
                }
            }
        }
    }
    private static List<MemoryRow> readPage(
            Connection connection, TableSpec table, Object cursor, int batchSize) throws SQLException {
        String sql = cursor == null
                ? "SELECT id, content, embedding::text, embedding_profile FROM public." + table.name()
                        + " ORDER BY id LIMIT ?"
                : "SELECT id, content, embedding::text, embedding_profile FROM public." + table.name()
                        + " WHERE id > ? ORDER BY id LIMIT ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int limitParameter;
            if (cursor == null) {
                limitParameter = 1;
            } else {
                statement.setObject(1, cursor);
                limitParameter = 2;
            }
            statement.setInt(limitParameter, batchSize);

            List<MemoryRow> rows = new ArrayList<>(batchSize);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new MemoryRow(
                            result.getObject(1),
                            result.getString(2),
                            result.getString(3),
                            result.getString(4)));
                }
            }
            return rows;
        }
    }

    private static boolean compareAndSet(
            Connection connection,
            TableSpec table,
            MemoryRow row,
            String newProfile,
            String vectorLiteral) throws SQLException {
        String sql = """
                UPDATE public.%s
                SET embedding = CAST(? AS vector), embedding_profile = ?
                WHERE id = ?
                  AND content IS NOT DISTINCT FROM ?
                  AND embedding IS NOT DISTINCT FROM CAST(? AS vector)
                  AND embedding_profile IS NOT DISTINCT FROM ?
                """.formatted(table.name());

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, vectorLiteral);
            statement.setString(2, newProfile);
            statement.setObject(3, row.id());
            statement.setString(4, row.content());
            setNullableString(statement, 5, row.embeddingText());
            setNullableString(statement, 6, row.profile());
            return statement.executeUpdate() == 1;
        }
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private static boolean isValidNormalizedVector(String vectorText) {
        if (vectorText == null) {
            return false;
        }
        String text = vectorText.trim();
        if (text.length() < 2 || text.charAt(0) != '[' || text.charAt(text.length() - 1) != ']') {
            return false;
        }
        String[] parts = text.substring(1, text.length() - 1).split(",", -1);
        if (parts.length != GeminiEmbeddingMigrationCli.EMBEDDING_DIMENSIONS) {
            return false;
        }

        double squaredNorm = 0.0;
        try {
            for (String part : parts) {
                float value = Float.parseFloat(part.trim());
                if (!Float.isFinite(value)) {
                    return false;
                }
                squaredNorm += (double) value * value;
            }
        } catch (NumberFormatException error) {
            return false;
        }

        double norm = Math.sqrt(squaredNorm);
        return Double.isFinite(norm) && norm > 0.0
                && Math.abs(norm - 1.0) <= NORMALIZED_NORM_TOLERANCE;
    }

    private static boolean isValidNormalizedVector(float[] values) {
        if (values == null || values.length != GeminiEmbeddingMigrationCli.EMBEDDING_DIMENSIONS) {
            return false;
        }
        double squaredNorm = 0.0;
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
            squaredNorm += (double) value * value;
        }
        double norm = Math.sqrt(squaredNorm);
        return Double.isFinite(norm) && norm > 0.0
                && Math.abs(norm - 1.0) <= NORMALIZED_NORM_TOLERANCE;
    }

    private static String vectorLiteral(float[] values) {
        StringBuilder builder = new StringBuilder(values.length * 8).append('[');
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(Float.toString(values[i]));
        }
        return builder.append(']').toString();
    }

    private static String failureType(Exception error) {
        String simpleName = error.getClass().getSimpleName();
        return simpleName.isBlank() ? "UnknownFailure" : simpleName;
    }

    private record TableSpec(String name, String expectedIdType) {
    }

    private record ColumnMetadata(String dataType, String udtName, String nullable, Integer maximumLength) {
    }

    private record MemoryRow(Object id, String content, String embeddingText, String profile) {
    }

    private static final class InvalidEmbeddingException extends RuntimeException {
        private InvalidEmbeddingException() {
            super("Embedding must be 1536 finite values with unit L2 norm");
        }
    }
}

record BackfillSummary(
        boolean apply,
        String profile,
        Map<String, TableBackfillSummary> tables,
        boolean limitReached) {

    BackfillSummary {
        tables = Map.copyOf(tables);
    }

    long scanned() {
        return tables.values().stream().mapToLong(TableBackfillSummary::scanned).sum();
    }

    long candidates() {
        return tables.values().stream().mapToLong(TableBackfillSummary::candidates).sum();
    }

    long updated() {
        return tables.values().stream().mapToLong(TableBackfillSummary::updated).sum();
    }

    long skipped() {
        return tables.values().stream().mapToLong(TableBackfillSummary::skipped).sum();
    }

    long conflicts() {
        return tables.values().stream().mapToLong(TableBackfillSummary::conflicts).sum();
    }

    long failures() {
        return tables.values().stream().mapToLong(TableBackfillSummary::failures).sum();
    }

    long missingTables() {
        return tables.values().stream().filter(table -> !table.present()).count();
    }
}

record TableBackfillSummary(
        String name,
        boolean present,
        long scanned,
        long candidates,
        long updated,
        long skipped,
        long conflicts,
        long failures,
        Map<String, Long> failureTypes) {

    TableBackfillSummary {
        failureTypes = Map.copyOf(failureTypes);
    }

    static TableBackfillSummary missing(String name) {
        return new TableBackfillSummary(name, false, 0, 0, 0, 0, 0, 0, Map.of());
    }

    static TableBackfillSummary empty(String name) {
        return new TableBackfillSummary(name, true, 0, 0, 0, 0, 0, 0, Map.of());
    }
}

final class MigrationSchemaException extends SQLException {
    MigrationSchemaException() {
        super("Required columns id, content, embedding and embedding_profile are not available");
    }
}