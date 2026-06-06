package com.labassist.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Key material for field-level encryption of patient PII at rest.
 */
@ConfigurationProperties("labassist.crypto")
public record CryptoProperties(
        /** Base64 of a 32-byte AES key (preferred) or any passphrase (SHA-256 derived). */
        String piiKey) {
}
