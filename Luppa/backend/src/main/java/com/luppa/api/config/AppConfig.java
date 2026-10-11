package com.luppa.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        CorsProperties.class,
        JwtProperties.class,
        StorageProperties.class,
        GeocodingProperties.class,
        AdminBootstrapProperties.class,
        EmailProperties.class,
        ProductionSafetyProperties.class,
        RateLimitProperties.class
})
public class AppConfig {
}
