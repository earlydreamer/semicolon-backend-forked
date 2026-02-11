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
        "dukku.semicolon",
        "dukku.common"
})
public class Application {

    static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
