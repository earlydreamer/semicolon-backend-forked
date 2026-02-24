package dukku.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.bootstrap-servers=localhost:19092"
})
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    void contextLoads() {
    }

}
