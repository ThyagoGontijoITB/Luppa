package com.luppa.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final StorageProperties storageProperties;

    public WebConfig(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path storagePath = Path.of(storageProperties.localDirectory()).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/events/**")
                .addResourceLocations(storagePath.toUri().toString());

        Path profileStoragePath = Path.of(storageProperties.profileLocalDirectory()).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/profile/**")
                .addResourceLocations(profileStoragePath.toUri().toString());
    }
}
