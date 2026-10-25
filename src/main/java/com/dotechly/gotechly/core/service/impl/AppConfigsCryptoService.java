package com.dotechly.gotechly.core.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypt-at-rest / decrypt-on-read for sensitive {@code app_configs} values.
 *
 * <p>AES-256-GCM with a random 12-byte IV per encryption; ciphertext is stored as
 * {@code enc:v1:Base64(IV || ciphertext || GCM-tag)}. The {@code enc:v1:} sentinel
 * makes both {@link #encryptIfNeeded} and {@link #decryptIfNeeded} idempotent — a
 * value is never double-encrypted, and legacy plaintext rows read straight through.
 * The version segment leaves room for a future key rotation.
 *
 * <p>The key is dedicated to app-config secrets (separate from the partner
 * {@code FieldEncryptionService} key) so the two can rotate independently. It is
 * derived via SHA-256 of {@code gotechly.appconfigs.encryption.secret-key}.
 *
 * <p>Plaintext exists only (a) transiently in the maker's submit request and
 * (b) in a plugin's memory at consumption. It is never at rest in {@code app_configs},
 * {@code pending_changes}, or {@code audit_log}, and is never returned to the Gateway.
 */
@Service
public class AppConfigsCryptoService {

    private static final Logger log = LoggerFactory.getLogger(AppConfigsCryptoService.class);

    /** Prefix marking an already-encrypted, versioned value. */
    public static final String SENTINEL = "enc:v1:";

    /** Placeholder returned to the portal in place of a secret's real value. */
    public static final String MASKED_VALUE = "********";

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AppConfigsCryptoService(@Value("${gotechly.appconfigs.encryption.secret-key}") String configuredKey) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(configuredKey.getBytes(StandardCharsets.UTF_8));
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize app-configs encryption key", e);
        }
    }

    /** True if the value is already an {@code enc:v1:} ciphertext. */
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(SENTINEL);
    }

    /**
     * True when an incoming secret value should be treated as "unchanged" —
     * null, blank, or the masked placeholder the portal echoes back untouched.
     */
    public boolean isBlankOrMasked(String value) {
        return value == null || value.isBlank() || MASKED_VALUE.equals(value.trim());
    }

    /**
     * Encrypts {@code plaintext} unless it is blank or already encrypted (idempotent).
     * Returns a {@code enc:v1:...} string.
     */
    public String encryptIfNeeded(String plaintext) {
        if (plaintext == null || plaintext.isEmpty() || isEncrypted(plaintext)) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return SENTINEL + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("app-configs encryption failed", e);
        }
    }

    /**
     * Decrypts an {@code enc:v1:...} value; returns anything else unchanged, so a
     * legacy plaintext row (marked sensitive but not yet re-encrypted) still reads.
     */
    public String decryptIfNeeded(String value) {
        if (!isEncrypted(value)) {
            return value;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(value.substring(SENTINEL.length()));

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Never surface ciphertext or key material; fail loud with context only.
            log.error("app-configs decryption failed — returning null for this value", e);
            throw new IllegalStateException("app-configs decryption failed", e);
        }
    }
}
