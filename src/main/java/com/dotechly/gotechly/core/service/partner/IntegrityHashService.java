package com.dotechly.gotechly.core.service.partner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * HMAC-SHA256 integrity hash service for partner data tamper detection.
 *
 * Uses a secret key so that only the application can produce valid hashes —
 * direct database modifications are detectable because the attacker cannot
 * recompute the hash without knowing the key.
 *
 * Partner hash: HMAC-SHA256(partnerCode | clientShortName | agentId | contactEmail |
 *                           contactPhone | clientCertificate | salt | callbackUrl |
 *                           ipWhitelist | partnerType)
 *
 * PartnerAccount hash: HMAC-SHA256(partner.hash1 | accountNumber | accountType |
 *                                   bankCode | currencyCode | isPrimary)
 */
@Service
public class IntegrityHashService {

    private static final Logger log = LoggerFactory.getLogger(IntegrityHashService.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String FIELD_SEPARATOR = "|";

    private final SecretKeySpec hmacKey;

    public IntegrityHashService(@Value("${gotechly.partner.encryption.secret-key}") String configuredKey) {
        try {
            // Derive a separate HMAC key from the configured secret using SHA-256
            // Using a different derivation context than FieldEncryptionService
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(("hmac-integrity:" + configuredKey).getBytes(StandardCharsets.UTF_8));
            this.hmacKey = new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize HMAC key", e);
        }
    }

    /**
     * Computes HMAC-SHA256 hash for a partner entity using all critical fields.
     *
     * @param partnerCode       unique partner code
     * @param clientShortName   unique short name
     * @param agentId           unique agent identifier
     * @param contactEmail      contact email
     * @param contactPhone      contact phone (nullable)
     * @param clientCertificate plaintext PEM certificate (nullable, before encryption)
     * @param salt              plaintext salt (before encryption)
     * @param callbackUrl       callback URL (nullable)
     * @param ipWhitelist       IP whitelist (nullable)
     * @param partnerType       partner type
     * @return lowercase hex HMAC-SHA256 hash (64 chars)
     */
    public String computePartnerHash(String partnerCode, String clientShortName, String agentId,
                                     String contactEmail, String contactPhone,
                                     String clientCertificate, String salt,
                                     String callbackUrl, String ipWhitelist, String partnerType) {
        String input = String.join(FIELD_SEPARATOR,
            safe(partnerCode),
            safe(clientShortName),
            safe(agentId),
            safe(contactEmail),
            safe(contactPhone),
            safe(clientCertificate),
            safe(salt),
            safe(callbackUrl),
            safe(ipWhitelist),
            safe(partnerType)
        );
        return hmac(input);
    }

    /**
     * Computes HMAC-SHA256 hash for a partner account entity.
     *
     * @param partnerHash1  the parent partner's hash1 value (chains integrity)
     * @param accountNumber account number
     * @param accountType   account type (SETTLEMENT, FEE, etc.)
     * @param bankCode      bank code (nullable)
     * @param currencyCode  currency code
     * @param isPrimary     primary account flag
     * @return lowercase hex HMAC-SHA256 hash (64 chars)
     */
    public String computeAccountHash(String partnerHash1, String accountNumber, String accountType,
                                     String bankCode, String currencyCode, Boolean isPrimary) {
        String input = String.join(FIELD_SEPARATOR,
            safe(partnerHash1),
            safe(accountNumber),
            safe(accountType),
            safe(bankCode),
            safe(currencyCode),
            isPrimary != null ? isPrimary.toString() : ""
        );
        return hmac(input);
    }

    /**
     * Verifies a stored hash against recomputed hash.
     *
     * @param storedHash   the hash stored in the database
     * @param computedHash the freshly computed hash
     * @return true if hashes match (no tampering detected)
     */
    public boolean verify(String storedHash, String computedHash) {
        if (storedHash == null || computedHash == null) {
            return false;
        }
        // Constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
            storedHash.getBytes(StandardCharsets.UTF_8),
            computedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String hmac(String input) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);
            byte[] hash = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
