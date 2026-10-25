package com.dotechly.gotechly.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfiguration implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:8080", "https://localhost:4200", "https://localhost:8080")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("Authorization", "Content-Type", "Accept", "X-Total-Count")
                .exposedHeaders("Authorization", "X-Total-Count")
                .allowCredentials(true)
                .maxAge(3600);
        registry.addMapping("/actuator/**")
                .allowedOrigins("http://localhost:8080")
                .allowedMethods("GET")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
