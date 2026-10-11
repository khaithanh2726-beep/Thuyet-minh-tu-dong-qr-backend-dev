package com.qraudio.backend.config;
import com.qraudio.backend.service.DemoDataService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
@Configuration
@Profile("!test")
public class DemoDataConfig {
    @Bean
    @ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
    CommandLineRunner sampleData(DemoDataService service) { return args -> service.seed(); }
}
