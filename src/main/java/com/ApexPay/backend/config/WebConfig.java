package com.ApexPay.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Using "/**" ensures both /api/employees and /api/logs are allowed
                registry.addMapping("/**") 
                        .allowedOrigins("https://apex-pay-frontend.vercel.app", "http://localhost:4200") 
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true) // Required for SSE streaming
                        .exposedHeaders("Content-Type"); 
            }
        };
    }
}
