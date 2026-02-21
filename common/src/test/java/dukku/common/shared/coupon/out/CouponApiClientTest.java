package dukku.common.shared.coupon.out;

import com.sun.net.httpserver.HttpServer;
import dukku.common.shared.coupon.dto.CouponInternalResponse;
import dukku.common.shared.coupon.exception.CouponNotFoundException;
import dukku.common.shared.coupon.exception.CouponUseNotAllowedException;
import dukku.common.shared.payment.exception.AmountMismatchException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponApiClientTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("200 응답이면 쿠폰 정보를 반환한다")
    void returnsCouponInfoOnSuccess() {
        UUID couponUuid = UUID.randomUUID();
        registerResponse(couponUuid, 200, """
                {
                  "discountAmount": 3000,
                  "minimumOrderAmount": 10000,
                  "status": "ACTIVE"
                }
                """);

        CouponApiClient client = new CouponApiClient(baseUrl);

        CouponInternalResponse response = client.getCouponInfo(couponUuid);

        assertThat(response.discountAmount()).isEqualTo(3000);
        assertThat(response.minimumOrderAmount()).isEqualTo(10000);
        assertThat(response.status().name()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("404는 CouponNotFoundException으로 매핑한다")
    void maps404ToCouponNotFoundException() {
        UUID couponUuid = UUID.randomUUID();
        registerResponse(couponUuid, 404, "{}");

        CouponApiClient client = new CouponApiClient(baseUrl);

        assertThatThrownBy(() -> client.getCouponInfo(couponUuid))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    @DisplayName("409는 CouponUseNotAllowedException으로 매핑한다")
    void maps409ToCouponUseNotAllowedException() {
        UUID couponUuid = UUID.randomUUID();
        registerResponse(couponUuid, 409, "{}");

        CouponApiClient client = new CouponApiClient(baseUrl);

        assertThatThrownBy(() -> client.getCouponInfo(couponUuid))
                .isInstanceOf(CouponUseNotAllowedException.class);
    }

    @Test
    @DisplayName("기타 4xx는 AmountMismatchException으로 매핑한다")
    void mapsOther4xxToAmountMismatchException() {
        UUID couponUuid = UUID.randomUUID();
        registerResponse(couponUuid, 400, "{}");

        CouponApiClient client = new CouponApiClient(baseUrl);

        assertThatThrownBy(() -> client.getCouponInfo(couponUuid))
                .isInstanceOf(AmountMismatchException.class);
    }

    @Test
    @DisplayName("5xx는 서버 오류로 전파한다")
    void propagates5xx() {
        UUID couponUuid = UUID.randomUUID();
        registerResponse(couponUuid, 500, "{}");

        CouponApiClient client = new CouponApiClient(baseUrl);

        assertThatThrownBy(() -> client.getCouponInfo(couponUuid))
                .isInstanceOf(HttpServerErrorException.class);
    }

    private void registerResponse(UUID couponUuid, int statusCode, String body) {
        server.createContext("/api/v1/internal/coupons/" + couponUuid, exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
    }
}
