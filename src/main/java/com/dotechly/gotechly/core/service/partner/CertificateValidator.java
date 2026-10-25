package com.dotechly.gotechly.core.service.partner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Validates PEM-encoded X.509 certificates uploaded by partners.
 * Accepts both raw Base64 content and full PEM format (with BEGIN/END markers).
 */
@Component
public class CertificateValidator {

    private static final Logger log = LoggerFactory.getLogger(CertificateValidator.class);

    private static final String PEM_HEADER = "-----BEGIN CERTIFICATE-----";
    private static final String PEM_FOOTER = "-----END CERTIFICATE-----";

    /**
     * Validates that the provided content is a valid PEM-encoded X.509 certificate.
     *
     * @param pemContent the PEM certificate content (with or without BEGIN/END markers)
     * @throws IllegalArgumentException if the content is not a valid PEM certificate
     */
    public void validate(String pemContent) {
        if (pemContent == null || pemContent.isBlank()) {
            throw new IllegalArgumentException("Certificate content is empty");
        }

        String normalized = normalizePem(pemContent);

        // Validate Base64 encoding
        String base64Only = normalized
            .replace(PEM_HEADER, "")
            .replace(PEM_FOOTER, "")
            .replaceAll("\\s+", "");

        try {
            Base64.getDecoder().decode(base64Only);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Certificate content is not valid Base64 encoding", e);
        }

        // Parse as X.509 certificate
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8))
            );
            log.debug("Valid certificate: subject={}, issuer={}, expires={}",
                cert.getSubjectX500Principal(), cert.getIssuerX500Principal(), cert.getNotAfter());
        } catch (CertificateException e) {
            throw new IllegalArgumentException("Invalid X.509 certificate: " + e.getMessage(), e);
        }
    }

    /**
     * Normalizes PEM content by ensuring BEGIN/END markers are present.
     * If the input is raw Base64 without markers, wraps it in standard PEM format.
     *
     * @param pemContent the raw or PEM-formatted certificate content
     * @return properly formatted PEM string
     */
    public String normalizePem(String pemContent) {
        String trimmed = pemContent.trim();
        if (trimmed.startsWith(PEM_HEADER)) {
            return trimmed;
        }
        // Raw Base64 — wrap with PEM markers
        return PEM_HEADER + "\n" + trimmed + "\n" + PEM_FOOTER;
    }
}
