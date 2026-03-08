package com.alvaro.pricewise.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alvaro.pricewise.exception.BadRequestException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ApiKeyEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKeySpec secretKey;

    public ApiKeyEncryptionService(
            @Value("${pricewise.api-keys.encryption-key:}") String encryptionKey) {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            log.warn("API_KEYS_ENCRYPTION_KEY no configurada. Usando key por defecto (solo desarrollo)");
            encryptionKey = "PriceWiseDevKey!PriceWiseDevKey!"; // 32 bytes = AES-256
        }
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "API_KEYS_ENCRYPTION_KEY debe tener exactamente 32 caracteres (AES-256)");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + encrypted concatenados y codificados en Base64
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            log.error("Error cifrando API key: {}", e.getMessage());
            throw new BadRequestException("Error al procesar la API key");
        }
    }

    public String decrypt(String encryptedText) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            log.error("Error descifrando API key: {}", e.getMessage());
            throw new BadRequestException("Error al leer la API key");
        }
    }

    public String mask(String plainText) {
        if (plainText == null || plainText.length() <= 4) {
            return "****";
        }
        return "****" + plainText.substring(plainText.length() - 4);
    }
}
