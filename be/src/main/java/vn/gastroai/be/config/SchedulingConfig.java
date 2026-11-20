package vn.gastroai.be.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;


@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.restore.run", havingValue = "false", matchIfMissing = true)
public class SchedulingConfig {
}