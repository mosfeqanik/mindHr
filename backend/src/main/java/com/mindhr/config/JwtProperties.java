package com.mindhr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code jwt.*} properties. {@code secret} must be a Base64-encoded key of at
 * least 256 bits (32 bytes) for HMAC-SHA256, supplied via the {@code JWT_SECRET} env var —
 * never commit a literal value.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expirationMs) {
}
