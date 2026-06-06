package com.labassist.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Transparently encrypts/decrypts a String column via {@link EncryptionService}.
 *
 * <p>Registered as a Spring bean so Hibernate (whose bean container Spring Boot
 * wires to the application context) injects the encryption service. Applied to
 * patient PII fields with {@code @Convert(converter = PiiAttributeConverter.class)}.
 */
@Component
@Converter
public class PiiAttributeConverter implements AttributeConverter<String, String> {

    private final EncryptionService encryptionService;

    public PiiAttributeConverter(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decrypt(dbData);
    }
}
