package dukku.deposit.boundedContext.deposit.in;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dukku.deposit.boundedContext.deposit.entity.Deposit;
import dukku.deposit.boundedContext.deposit.entity.DepositHistory;
import dukku.deposit.boundedContext.deposit.out.DepositHistoryRepository;
import dukku.deposit.boundedContext.deposit.out.DepositRepository;
import dukku.deposit.global.SystemDepositInitData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:deposit_internal_e2e;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.elasticsearch.uris=http://localhost:9200",
        "jwt.access.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktYWNjZXNzLTAxMjM=",
        "jwt.refresh.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktcmVmcmVzaC0wMTI=",
        "crypto.key=dGhpcy1rZXktaXMtdGVzdC1rZXktY3J5cHRvLTAxMjM="
})
@ActiveProfiles("test")
class DepositInternalControllerE2ETest {

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    @Autowired
    private DepositRepository depositRepository;

    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    @BeforeEach
    void cleanUp() {
        depositHistoryRepository.deleteAll();
        depositRepository.deleteAll();
    }

    @Test
    @DisplayName("동일 settlementUuid로 내부 충전을 두 번 호출하면 한 번만 반영된다")
    void chargeForSettlementIsIdempotentOnDuplicateSettlementUuid() throws Exception {
        // given: 사용자 지갑과 시스템 지갑이 준비되어 있고, 같은 정산 UUID로 두 번 호출할 요청 본문이 있다.
        UUID userUuid = UUID.randomUUID();
        UUID settlementUuid = UUID.randomUUID();
        Long amount = 2000L;

        depositRepository.save(Deposit.builder()
                .userUuid(userUuid)
                .depositUuid(UUID.randomUUID())
                .balance(1000L)
                .version(0)
                .build());
        depositRepository.save(Deposit.builder()
                .userUuid(SystemDepositInitData.SYSTEM_USER_UUID)
                .depositUuid(UUID.randomUUID())
                .balance(1_000_000L)
                .version(0)
                .build());

        Map<String, Object> bodyMap = Map.of(
                "amount", amount,
                "settlementUuid", settlementUuid
        );
        String body = objectMapper.writeValueAsString(bodyMap);
        String url = "http://localhost:" + port + "/api/v1/internal/deposits/" + userUuid + "/charge";

        // when: 동일한 settlementUuid로 내부 충전 API를 연속 두 번 호출한다.
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> firstResponse = postJson(client, url, body);
        HttpResponse<String> secondResponse = postJson(client, url, body);
        JsonNode firstJson = objectMapper.readTree(firstResponse.body());
        JsonNode secondJson = objectMapper.readTree(secondResponse.body());

        // then: 사용자/시스템 잔액과 이력은 1회 충전 기준으로만 반영된다.
        assertThat(firstResponse.statusCode()).isEqualTo(200);
        assertThat(secondResponse.statusCode()).isEqualTo(200);
        assertThat(firstJson.get("success").asBoolean()).isTrue();
        assertThat(secondJson.get("success").asBoolean()).isTrue();
        assertThat(firstJson.get("code").asText()).isEqualTo("DEPOSIT_CHARGED");
        assertThat(secondJson.get("code").asText()).isEqualTo("DEPOSIT_CHARGED");

        Deposit userDeposit = depositRepository.findByUserUuid(userUuid).orElseThrow();
        Deposit systemDeposit = depositRepository.findByUserUuid(SystemDepositInitData.SYSTEM_USER_UUID)
                .orElseThrow();
        assertThat(userDeposit.getBalance()).isEqualTo(3000L);
        assertThat(systemDeposit.getBalance()).isEqualTo(998000L);

        List<DepositHistory> histories = depositHistoryRepository.findByOrderItemUuid(settlementUuid);
        assertThat(histories).hasSize(2);
    }

    private HttpResponse<String> postJson(HttpClient client, String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
