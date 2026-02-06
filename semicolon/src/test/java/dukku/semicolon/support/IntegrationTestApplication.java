package dukku.semicolon.support;

import dukku.semicolon.global.SystemDepositInitData;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = "dukku.semicolon",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SystemDepositInitData.class)
)
public class IntegrationTestApplication {
}
