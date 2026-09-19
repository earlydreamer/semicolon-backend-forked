package dukku.ai.app.migration;

import java.io.PrintStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import dukku.ai.app.service.GeminiEmbeddingService;
import dukku.ai.global.policy.AiSimilarityPolicy;

/** Standalone migration entry point. It deliberately does not start Spring Boot. */
public final class GeminiEmbeddingMigrationCli {

    static final int EMBEDDING_DIMENSIONS = AiSimilarityPolicy.EMBEDDING_DIMENSION;
    private static final String DEFAULT_MODEL = "gemini-embedding-001";
    private static final int REQUEST_TIMEOUT_MILLIS = 60_000;
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 1_000;

    private GeminiEmbeddingMigrationCli() {
    }

    public static void main(String[] args) {
        int exitCode = execute(
                args,
                System.getenv(),
                System.out,
                GeminiEmbeddingMigrationCli::openDatabase,
                GeminiEmbeddingMigrationCli::openGeminiEmbedder);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int execute(
            String[] args,
            Map<String, String> environment,
            PrintStream output,
            DatabaseConnectionFactory databaseFactory,
            EmbeddingProviderFactory embeddingFactory) {
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException error) {
            output.println("Invalid arguments. Use --help to see supported options.");
            return 2;
        }
        if (options.help()) {
            printUsage(output);
            return 0;
        }

        String model = nonBlank(environment.get("EMBEDDING_MODEL"), DEFAULT_MODEL);
        String profile;
        try {
            profile = GeminiEmbeddingService.profileFor(model, EMBEDDING_DIMENSIONS);
            if (profile.length() > 160) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException error) {
            output.println("Invalid EMBEDDING_MODEL configuration for the nullable 160-character profile.");
            return 2;
        }

        String apiKey = null;
        if (options.apply()) {
            apiKey = environment.get("GEMINI_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                output.println("--apply requires the existing GEMINI_API_KEY environment variable.");
                return 2;
            }
        }

        try (Connection connection = databaseFactory.open(environment)) {
            GeminiEmbeddingBackfill.preflight(connection);
            if (options.apply()) {
                try (DocumentEmbedder embedder = embeddingFactory.open(apiKey, model)) {
                    return runBackfill(connection, options, profile, embedder, output);
                }
            }
            return runBackfill(connection, options, profile, null, output);
        } catch (MigrationSchemaException error) {
            output.println("Required tables or 1536-dimensional profile schema are missing; initialize both tables and apply sql/manual/20260919_gemini_embedding_profile.sql first.");
            return 2;
        } catch (Exception error) {
            output.println("Migration stopped before completion (" + safeType(error) + ").");
            return 1;
        }
    }

    private static int runBackfill(
            Connection connection,
            Options options,
            String profile,
            DocumentEmbedder embedder,
            PrintStream output) throws SQLException {
        BackfillSummary summary = GeminiEmbeddingBackfill.run(
                connection,
                options.apply(),
                options.batchSize(),
                options.maxRows(),
                profile,
                embedder);
        printSummary(summary, output);
        return summary.failures() == 0 && summary.conflicts() == 0 ? 0 : 1;
    }

    private static Connection openDatabase(Map<String, String> environment) throws SQLException {
        String host = nonBlank(environment.get("DB_HOST"), "postgres");
        String portValue = nonBlank(environment.get("DB_PORT"), "5432");
        String database = nonBlank(environment.get("DB_NAME"), "ai_service");
        String sslMode = nonBlank(environment.get("DB_SSLMODE"), "disable").toLowerCase(Locale.ROOT);

        int port;
        try {
            port = Integer.parseInt(portValue);
        } catch (NumberFormatException error) {
            throw new SQLException("Invalid DB_PORT configuration");
        }
        if (port < 1 || port > 65_535 || !isSupportedSslMode(sslMode)) {
            throw new SQLException("Invalid database connection configuration");
        }

        String urlHost = host.indexOf(':') >= 0 && !host.startsWith("[") ? "[" + host + "]" : host;
        String encodedDatabase = URLEncoder.encode(database, StandardCharsets.UTF_8).replace("+", "%20");
        String jdbcUrl = "jdbc:postgresql://%s:%d/%s?sslmode=%s".formatted(
                urlHost, port, encodedDatabase, sslMode);
        Properties properties = new Properties();
        String username = environment.get("DB_USERNAME");
        String password = environment.get("DB_PASSWORD");
        if (username != null && !username.isBlank()) {
            properties.setProperty("user", username);
        }
        if (password != null) {
            properties.setProperty("password", password);
        }
        return DriverManager.getConnection(jdbcUrl, properties);
    }

    private static DocumentEmbedder openGeminiEmbedder(String apiKey, String model) {
        Client client = Client.builder()
                .apiKey(apiKey)
                .vertexAI(false)
                .httpOptions(HttpOptions.builder()
                        .timeout(REQUEST_TIMEOUT_MILLIS)
                        .build())
                .build();
        try {
            GeminiEmbeddingService service = new GeminiEmbeddingService(client, model, EMBEDDING_DIMENSIONS);
            return new DocumentEmbedder() {
                @Override
                public float[] embedDocument(String content) {
                    return service.embedDocument(content);
                }

                @Override
                public void close() {
                    client.close();
                }
            };
        } catch (RuntimeException error) {
            client.close();
            throw error;
        }
    }

    private static boolean isSupportedSslMode(String mode) {
        return switch (mode) {
            case "disable", "allow", "prefer", "require", "verify-ca", "verify-full" -> true;
            default -> false;
        };
    }

    private static String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private static String safeType(Exception error) {
        String simpleName = error.getClass().getSimpleName();
        return simpleName.isBlank() ? "MigrationError" : simpleName;
    }

    private static void printSummary(BackfillSummary summary, PrintStream output) {
        output.printf(
                "mode=%s profile=%s scanned=%d candidates=%d updated=%d skipped=%d conflicts=%d failures=%d missing_tables=%d limit_reached=%s%n",
                summary.apply() ? "apply" : "dry-run",
                summary.profile(),
                summary.scanned(),
                summary.candidates(),
                summary.updated(),
                summary.skipped(),
                summary.conflicts(),
                summary.failures(),
                summary.missingTables(),
                summary.limitReached());
        for (String tableName : new String[] {"ai_user_memory", "product_search"}) {
            TableBackfillSummary table = summary.tables().get(tableName);
            if (table == null) {
                continue;
            }
            output.printf(
                    "table=%s present=%s scanned=%d candidates=%d updated=%d skipped=%d conflicts=%d failures=%d%n",
                    table.name(),
                    table.present(),
                    table.scanned(),
                    table.candidates(),
                    table.updated(),
                    table.skipped(),
                    table.conflicts(),
                    table.failures());
            if (!table.failureTypes().isEmpty()) {
                output.printf("table=%s failure_types=%s%n", table.name(), formatFailureTypes(table.failureTypes()));
            }
        }
    }

    private static String formatFailureTypes(Map<String, Long> failureTypes) {
        return failureTypes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static void printUsage(PrintStream output) {
        output.println("Gemini embedding profile migration");
        output.println("  (default) --dry-run                 Read rows and report the work without API calls or writes");
        output.println("  --apply                              Recreate embeddings and atomically update vector/profile");
        output.println("  --batch-size <1-1000>                Maximum rows fetched per keyset page (default: 100)");
        output.println("  --max-rows <positive integer>        Maximum candidate rows attempted across both tables");
        output.println("  --help                               Show this help");
        output.println("Database settings use DB_HOST/PORT/NAME/USERNAME/PASSWORD/SSLMODE. --apply uses GEMINI_API_KEY.");
    }

    @FunctionalInterface
    interface DatabaseConnectionFactory {
        Connection open(Map<String, String> environment) throws SQLException;
    }

    @FunctionalInterface
    interface EmbeddingProviderFactory {
        DocumentEmbedder open(String apiKey, String model);
    }

    @FunctionalInterface
    interface DocumentEmbedder extends AutoCloseable {
        float[] embedDocument(String content);

        @Override
        default void close() {
        }
    }

    private record Options(boolean apply, int batchSize, long maxRows, boolean help) {

        static Options parse(String[] args) {
            boolean apply = false;
            boolean dryRun = false;
            boolean help = false;
            int batchSize = DEFAULT_BATCH_SIZE;
            long maxRows = Long.MAX_VALUE;

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--apply" -> apply = true;
                    case "--dry-run" -> dryRun = true;
                    case "--help", "-h" -> help = true;
                    case "--batch-size" -> {
                        if (++i >= args.length) {
                            throw new IllegalArgumentException();
                        }
                        batchSize = parsePositiveInt(args[i]);
                        if (batchSize > MAX_BATCH_SIZE) {
                            throw new IllegalArgumentException();
                        }
                    }
                    case "--max-rows" -> {
                        if (++i >= args.length) {
                            throw new IllegalArgumentException();
                        }
                        maxRows = parsePositiveLong(args[i]);
                    }
                    default -> throw new IllegalArgumentException();
                }
            }
            if (apply && dryRun) {
                throw new IllegalArgumentException();
            }
            return new Options(apply, batchSize, maxRows, help);
        }

        private static int parsePositiveInt(String value) {
            try {
                int parsed = Integer.parseInt(value);
                if (parsed < 1) {
                    throw new IllegalArgumentException();
                }
                return parsed;
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException();
            }
        }

        private static long parsePositiveLong(String value) {
            try {
                long parsed = Long.parseLong(value);
                if (parsed < 1) {
                    throw new IllegalArgumentException();
                }
                return parsed;
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException();
            }
        }
    }
}