package dukku.common.global.auth;

public final class InternalServiceTokenResolver {

    public static final String PROPERTY_NAME = "INTERNAL_SERVICE_TOKEN";
    public static final String HEADER_NAME = "X-Internal-Service-Token";

    private InternalServiceTokenResolver() {
    }

    public static String resolve() {
        String propertyValue = System.getProperty(PROPERTY_NAME);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        String envValue = System.getenv(PROPERTY_NAME);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        return null;
    }
}
