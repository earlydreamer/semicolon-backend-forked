package dukku.common.global.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InternalServiceTokenResolverTest {

    private static final String PROPERTY_NAME = "INTERNAL_SERVICE_TOKEN";

    @AfterEach
    void tearDown() {
        System.clearProperty(PROPERTY_NAME);
    }

    @Test
    @DisplayName("시스템 프로퍼티에 INTERNAL_SERVICE_TOKEN이 있으면 trim 후 반환한다")
    void resolvesTokenFromSystemProperty() {
        System.setProperty(PROPERTY_NAME, "  test-internal-token  ");

        String resolved = InternalServiceTokenResolver.resolve();

        assertThat(resolved).isEqualTo("test-internal-token");
    }

    @Test
    @DisplayName("설정된 INTERNAL_SERVICE_TOKEN이 없으면 null을 반환한다")
    void returnsNullWhenTokenMissing() {
        String resolved = InternalServiceTokenResolver.resolve();

        assertThat(resolved).isNull();
    }
}
