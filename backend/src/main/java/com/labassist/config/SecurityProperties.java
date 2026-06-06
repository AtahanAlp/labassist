package com.labassist.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Authentication, CORS and bootstrap-account settings.
 */
@ConfigurationProperties("labassist.security")
public record SecurityProperties(
        Jwt jwt,
        Cors cors,
        Seed seed) {

    /** JSON Web Token signing configuration. */
    public record Jwt(String secret, long expirationMinutes) {
    }

    /** Cross-origin settings for the browser SPA. */
    public record Cors(List<String> allowedOrigins) {
    }

    /** Accounts created on first boot if they do not already exist. */
    public record Seed(
            String doctorUsername,
            String doctorPassword,
            String doctorDisplayName,
            String adminUsername,
            String adminPassword,
            String adminDisplayName) {
    }
}
