package com.company.expensetracker.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class AesKeyHolder {

    @Value("${app.crypto.aes-key}")
    private String aesKeyBase64;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        if (aesKeyBase64 == null || aesKeyBase64.isBlank()) {
            throw new BeanCreationException("app.crypto.aes-key must be set (base64-encoded 32-byte key)");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(aesKeyBase64);
        } catch (IllegalArgumentException e) {
            throw new BeanCreationException("app.crypto.aes-key is not valid base64", e);
        }
        if (keyBytes.length != 32) {
            throw new BeanCreationException(
                    "app.crypto.aes-key must decode to exactly 32 bytes (AES-256), got " + keyBytes.length);
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public SecretKey getKey() {
        return secretKey;
    }
}
