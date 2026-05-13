package com.company.expensetracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration for the Expense Tracker API.
 *
 * <p>Reads allowed origins from the {@code app.cors.allowed-origins} property list.
 * Wildcard origins ({@code *}) are intentionally unsupported — every origin must be
 * explicitly whitelisted. Credentials ({@code Authorization} cookie) are allowed by default.
 *
 * <p>Allowed methods: {@code GET, POST, PUT, PATCH, DELETE, OPTIONS}.
 * Allowed headers: {@code Authorization, Content-Type, Accept}.
 * Pre-flight cache: 3600 seconds.
 */
@Configuration
@ConfigurationProperties(prefix = "app.cors")
public class CorsConfig {

    private List<String> allowedOrigins = List.of();
    private boolean allowCredentials = true;

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = List.copyOf(allowedOrigins);
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    /**
     * Builds a {@link CorsConfigurationSource} that applies the configured allowed origins
     * and standard method/header rules to every path ({@code /**}).
     *
     * @return the configured CORS source, registered globally in the security filter chain
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
