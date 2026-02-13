package dukku.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableResilientMethods
@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = {
        "dukku.auth",
        "dukku.common"
})
public class AuthApplication {

    static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

}
