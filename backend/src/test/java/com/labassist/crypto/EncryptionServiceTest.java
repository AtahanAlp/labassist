package com.labassist.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.labassist.config.CryptoProperties;
import org.junit.jupiter.api.Test;

class EncryptionServiceTest {

    private static final String BASE64_KEY = "DJOxEDA9JiEEcrVM2jQkE5625vpBt3/9dwBQ0kWQ9AM=";

    private EncryptionService service(String key) {
        return new EncryptionService(new CryptoProperties(key));
    }

    @Test
    void roundTripReturnsOriginalPlaintext() {
        EncryptionService service = service(BASE64_KEY);
        String plaintext = "Ada Yilmaz";
        String encrypted = service.encrypt(plaintext);

        assertThat(encrypted).isNotNull().isNotEqualTo(plaintext);
        assertThat(service.decrypt(encrypted)).isEqualTo(plaintext);
    }

    @Test
    void encryptingSameValueTwiceProducesDifferentCiphertext() {
        EncryptionService service = service(BASE64_KEY);
        assertThat(service.encrypt("MRN-123456")).isNotEqualTo(service.encrypt("MRN-123456"));
    }

    @Test
    void nullIsPassedThrough() {
        EncryptionService service = service(BASE64_KEY);
        assertThat(service.encrypt(null)).isNull();
        assertThat(service.decrypt(null)).isNull();
    }

    @Test
    void arbitraryPassphraseKeyAlsoWorks() {
        EncryptionService service = service("any-passphrase-derived-via-sha256");
        assertThat(service.decrypt(service.encrypt("patient"))).isEqualTo("patient");
    }
}
