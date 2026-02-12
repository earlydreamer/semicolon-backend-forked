package dukku.coupon.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = IntegrationTestApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:integration-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.datasource.hikari.maximum-pool-size=20"
})
@Transactional
public abstract class IntegrationTestSupport {
}
