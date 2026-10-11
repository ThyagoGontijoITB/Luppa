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
        BrevoProperties.class,
        ProductionSafetyProperties.class,
        RateLimitProperties.class,
        CloudinaryProperties.class
})
public class AppConfig {
}
