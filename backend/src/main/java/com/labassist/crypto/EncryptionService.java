package com.labassist.crypto;

import com.labassist.config.CryptoProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Authenticated symmetric encryption (AES-256-GCM) for patient PII at rest.
 *
 * <p>Each value is encrypted with a fresh random 96-bit IV; the stored format is
 * {@code Base64(iv || ciphertext || gcmTag)}. GCM provides confidentiality and
 * integrity, so tampering with the stored ciphertext is detected on decrypt.
 */
@Service
public class EncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionService(CryptoProperties properties) {
        this.key = new SecretKeySpec(resolveKey(properties.piiKey()), "AES");
    }

    /**
     * Accepts either a Base64-encoded 32-byte key (preferred) or any passphrase,
     * which is hashed with SHA-256 to derive a 256-bit key.
     */
    private static byte[] resolveKey(String configured) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("labassist.crypto.pii-key must be configured");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(configured);
            if (decoded.length == 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Not valid Base64 — fall through to passphrase derivation.
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(configured.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to derive PII encryption key", e);
        }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt value", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored);
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt value", e);
        }
    }
}
