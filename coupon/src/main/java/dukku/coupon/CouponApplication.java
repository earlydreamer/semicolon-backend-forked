package dukku.coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableRetry
@EnableResilientMethods
@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = {
        "dukku.coupon",
        "dukku.common"
})
public class CouponApplication {

    static void main(String[] args) {
        SpringApplication.run(CouponApplication.class, args);
    }

}
