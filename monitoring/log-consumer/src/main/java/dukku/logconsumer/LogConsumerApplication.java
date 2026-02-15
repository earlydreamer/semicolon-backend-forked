package dukku.logconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = {"dukku.logconsumer", "dukku.common"},
        excludeName = {
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
                "org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration",
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
        }
)
public class LogConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogConsumerApplication.class, args);
    }
}
