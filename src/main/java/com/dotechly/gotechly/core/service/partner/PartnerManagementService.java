package com.dotechly.gotechly.core.service.partner;

import com.dotechly.gotechly.core.domain.Partner;
import com.dotechly.gotechly.core.repository.PartnerRepository;
import com.dotechly.gotechly.core.service.dto.PartnerDTO;
import com.dotechly.gotechly.core.service.mapper.PartnerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for partner CRUD and maker-checker workflow.
 *
 * Workflow:
 * 1. Maker creates partner → status=PENDING_APPROVAL, salt generated, cert+salt encrypted
 * 2. Checker approves → status=ACTIVE, email sent with unencrypted salt
 * 3. Checker rejects → status=REJECTED with remarks
 */
@Service
@Transactional
public class PartnerManagementService {

    private static final Logger log = LoggerFactory.getLogger(PartnerManagementService.class);

    public static final String CACHE_PARTNERS = "partners";

    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_DEACTIVATED = "DEACTIVATED";
    public static final String STATUS_REJECTED = "REJECTED";

    private final PartnerRepository partnerRepository;
    private final PartnerMapper partnerMapper;
    private final FieldEncryptionService encryptionService;
    private final SaltGenerator saltGenerator;
    private final CertificateValidator certificateValidator;
    private final PartnerEmailService emailService;
    private final IntegrityHashService integrityHashService;
    private final PartnerAuditLogger auditLogger;

    public PartnerManagementService(
            PartnerRepository partnerRepository,
            PartnerMapper partnerMapper,
            FieldEncryptionService encryptionService,
            SaltGenerator saltGenerator,
            CertificateValidator certificateValidator,
            PartnerEmailService emailService,
            IntegrityHashService integrityHashService,
            PartnerAuditLogger auditLogger) {
        this.partnerRepository = partnerRepository;
        this.partnerMapper = partnerMapper;
        this.encryptionService = encryptionService;
        this.saltGenerator = saltGenerator;
        this.certificateValidator = certificateValidator;
        this.emailService = emailService;
        this.integrityHashService = integrityHashService;
        this.auditLogger = auditLogger;
    }

    /**
     * Creates a new partner with PENDING_APPROVAL status.
     * Validates the PEM certificate, generates a random salt,
     * and encrypts both before storing.
     */
    @CacheEvict(value = CACHE_PARTNERS, allEntries = true)
    public PartnerDTO create(PartnerDTO dto) {
        log.debug("Request to create Partner: {}", dto);

        // Validate and normalize certificate if provided
        if (dto.getClientCertificate() != null && !dto.getClientCertificate().isBlank()) {
            certificateValidator.validate(dto.getClientCertificate());
            dto.setClientCertificate(certificateValidator.normalizePem(dto.getClientCertificate()));
        }

        // Generate random salt
        String plainSalt = saltGenerator.generate();

        Partner partner = partnerMapper.toEntity(dto);
        partner.setPartnerCode(generatePartnerCode());
        partner.setStatus(STATUS_PENDING_APPROVAL);
        partner.setMakerId(getCurrentUsername());
        partner.setMakerDate(Instant.now());
        partner.setDateCreated(Instant.now());
        partner.setCreatedBy(getCurrentUsername());

        // Compute HMAC integrity hash before encryption using all critical fields
        String plainCert = partner.getClientCertificate();
        partner.setHash1(integrityHashService.computePartnerHash(
            partner.getPartnerCode(), partner.getClientShortName(), partner.getAgentId(),
            partner.getContactEmail(), partner.getContactPhone(),
            plainCert, plainSalt,
            partner.getCallbackUrl(), partner.getIpWhitelist(), partner.getPartnerType()
        ));

        // Encrypt sensitive fields before persisting
        partner.setSalt(encryptionService.encrypt(plainSalt));
        if (plainCert != null && !plainCert.isBlank()) {
            partner.setClientCertificate(encryptionService.encrypt(plainCert));
        }

        partner = partnerRepository.save(partner);
        auditLogger.log("PARTNER_CREATED", "Partner", String.valueOf(partner.getId()),
            "code=" + partner.getPartnerCode() + ", shortName=" + partner.getClientShortName());
        return partnerMapper.toDto(partner);
    }

    /**
     * Updates an existing partner (re-submits for approval).
     * If the certificate is changed, validates and re-encrypts.
     * If the salt needs regeneration, pass regenerateSalt=true.
     */
    @CacheEvict(value = CACHE_PARTNERS, allEntries = true)
    public PartnerDTO update(PartnerDTO dto) {
        log.debug("Request to update Partner: {}", dto);

        Partner existing = partnerRepository.findById(dto.getId())
            .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + dto.getId()));

        // Validate certificate if changed
        if (dto.getClientCertificate() != null && !dto.getClientCertificate().isBlank()) {
            // If the DTO certificate doesn't look like an encrypted value, it's a new upload
            if (!dto.getClientCertificate().equals(existing.getClientCertificate())) {
                certificateValidator.validate(dto.getClientCertificate());
                dto.setClientCertificate(
                    encryptionService.encrypt(certificateValidator.normalizePem(dto.getClientCertificate()))
                );
            }
        }

        Partner partner = partnerMapper.toEntity(dto);
        partner.setDateModified(Instant.now());
        partner.setModifiedBy(getCurrentUsername());

        // Preserve encrypted salt from existing record (salt is only set at creation)
        if (partner.getSalt() == null || partner.getSalt().isBlank()) {
            partner.setSalt(existing.getSalt());
        }

        // Recompute HMAC integrity hash with current field values
        String plainSalt = encryptionService.decrypt(partner.getSalt());
        String plainCert;
        if (partner.getClientCertificate() != null && !partner.getClientCertificate().isBlank()) {
            // Certificate is already encrypted at this point (re-encrypted above or preserved)
            plainCert = encryptionService.decrypt(partner.getClientCertificate());
        } else {
            plainCert = null;
        }
        partner.setHash1(integrityHashService.computePartnerHash(
            partner.getPartnerCode(), partner.getClientShortName(), partner.getAgentId(),
            partner.getContactEmail(), partner.getContactPhone(),
            plainCert, plainSalt,
            partner.getCallbackUrl(), partner.getIpWhitelist(), partner.getPartnerType()
        ));

        partner = partnerRepository.save(partner);
        auditLogger.log("PARTNER_UPDATED", "Partner", String.valueOf(partner.getId()),
            "code=" + partner.getPartnerCode());
        return partnerMapper.toDto(partner);
    }

    /**
     * Checker approves a pending partner.
     * Decrypts the salt, sends credentials email, updates status to ACTIVE.
     */
    @CacheEvict(value = CACHE_PARTNERS, allEntries = true)
    public PartnerDTO approve(Long partnerId, String checkerId, String remarks) {
        log.debug("Request to approve Partner: {}", partnerId);

        Partner partner = partnerRepository.findById(partnerId)
            .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + partnerId));

        if (!STATUS_PENDING_APPROVAL.equals(partner.getStatus())) {
            throw new IllegalStateException("Partner is not in PENDING_APPROVAL status, current status: " + partner.getStatus());
        }

        if (checkerId.equals(partner.getMakerId())) {
            throw new IllegalStateException("Checker cannot be the same user as maker");
        }

        // Decrypt salt for email notification
        String plainSalt = encryptionService.decrypt(partner.getSalt());

        partner.setStatus(STATUS_ACTIVE);
        partner.setCheckerId(checkerId);
        partner.setCheckerDate(Instant.now());
        partner.setCheckerRemarks(remarks);
        partner.setDateModified(Instant.now());
        partner.setModifiedBy(checkerId);

        partner = partnerRepository.save(partner);

        // Send credentials email asynchronously
        emailService.sendApprovalNotification(
            partner.getContactEmail(),
            partner.getClientShortName(),
            partner.getAgentId(),
            plainSalt
        );

        auditLogger.log("PARTNER_APPROVED", "Partner", String.valueOf(partner.getId()),
            "checker=" + checkerId + ", remarks=" + remarks);
        return partnerMapper.toDto(partner);
    }

    /**
     * Checker rejects a pending partner.
     */
    @CacheEvict(value = CACHE_PARTNERS, allEntries = true)
    public PartnerDTO reject(Long partnerId, String checkerId, String remarks) {
        log.debug("Request to reject Partner: {}", partnerId);

        Partner partner = partnerRepository.findById(partnerId)
            .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + partnerId));

        if (!STATUS_PENDING_APPROVAL.equals(partner.getStatus())) {
            throw new IllegalStateException("Partner is not in PENDING_APPROVAL status, current status: " + partner.getStatus());
        }

        if (checkerId.equals(partner.getMakerId())) {
            throw new IllegalStateException("Checker cannot be the same user as maker");
        }

        partner.setStatus(STATUS_REJECTED);
        partner.setCheckerId(checkerId);
        partner.setCheckerDate(Instant.now());
        partner.setCheckerRemarks(remarks);
        partner.setDateModified(Instant.now());
        partner.setModifiedBy(checkerId);

        partner = partnerRepository.save(partner);
        auditLogger.log("PARTNER_REJECTED", "Partner", String.valueOf(partner.getId()),
            "checker=" + checkerId + ", remarks=" + remarks);
        return partnerMapper.toDto(partner);
    }

    /**
     * Suspends an active partner.
     */
    @CacheEvict(value = CACHE_PARTNERS, allEntries = true)
    public PartnerDTO suspend(Long partnerId, String userId, String remarks) {
        Partner partner = partnerRepository.findById(partnerId)
            .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + partnerId));

        if (!STATUS_ACTIVE.equals(partner.getStatus())) {
            throw new IllegalStateException("Only ACTIVE partners can be suspended");
        }

        partner.setStatus(STATUS_SUSPENDED);
        partner.setDateModified(Instant.now());
        partner.setModifiedBy(userId);
        partner.setCheckerRemarks(remarks);

        partner = partnerRepository.save(partner);
        auditLogger.log("PARTNER_SUSPENDED", "Partner", String.valueOf(partner.getId()),
            "userId=" + userId + ", remarks=" + remarks);
        return partnerMapper.toDto(partner);
    }

    /**
     * Reactivates a suspended partner.
     */
    @CacheEvict(value = CACHE_PARTNERS, allEntries = true)
    public PartnerDTO activate(Long partnerId, String userId, String remarks) {
        Partner partner = partnerRepository.findById(partnerId)
            .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + partnerId));

        if (!STATUS_SUSPENDED.equals(partner.getStatus())) {
            throw new IllegalStateException("Only SUSPENDED partners can be reactivated");
        }

        partner.setStatus(STATUS_ACTIVE);
        partner.setDateModified(Instant.now());
        partner.setModifiedBy(userId);
        partner.setCheckerRemarks(remarks);

        partner = partnerRepository.save(partner);
        auditLogger.log("PARTNER_ACTIVATED", "Partner", String.valueOf(partner.getId()),
            "userId=" + userId + ", remarks=" + remarks);
        return partnerMapper.toDto(partner);
    }

    @Transactional(readOnly = true)
    public Page<PartnerDTO> findAll(Pageable pageable) {
        return partnerRepository.findAll(pageable).map(partnerMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<PartnerDTO> findOne(Long id) {
        return partnerRepository.findById(id).map(partnerMapper::toDto);
    }

    @Cacheable(value = CACHE_PARTNERS, key = "#agentId")
    @Transactional(readOnly = true)
    public Optional<Partner> findByAgentId(String agentId) {
        Optional<Partner> result = partnerRepository.findByAgentId(agentId);
        result.ifPresent(this::verifyIntegrity);
        return result;
    }

    /**
     * Verifies the HMAC integrity hash of a partner record.
     * Decrypts salt and certificate to recompute the hash, then compares.
     *
     * @throws IllegalStateException if integrity check fails (data tampered)
     */
    public void verifyIntegrity(Partner partner) {
        if (partner.getHash1() == null) {
            log.warn("Partner {} has no integrity hash — skipping verification (legacy record)", partner.getPartnerCode());
            return;
        }

        String plainSalt = encryptionService.decrypt(partner.getSalt());
        String plainCert = null;
        if (partner.getClientCertificate() != null && !partner.getClientCertificate().isBlank()) {
            plainCert = encryptionService.decrypt(partner.getClientCertificate());
        }

        String computedHash = integrityHashService.computePartnerHash(
            partner.getPartnerCode(), partner.getClientShortName(), partner.getAgentId(),
            partner.getContactEmail(), partner.getContactPhone(),
            plainCert, plainSalt,
            partner.getCallbackUrl(), partner.getIpWhitelist(), partner.getPartnerType()
        );

        if (!integrityHashService.verify(partner.getHash1(), computedHash)) {
            log.error("INTEGRITY CHECK FAILED for partner {} (code: {}). Stored hash does not match computed hash.",
                partner.getId(), partner.getPartnerCode());
            throw new IllegalStateException("Data integrity check failed for partner: " + partner.getPartnerCode());
        }
    }

    @CacheEvict(value = CACHE_PARTNERS, allEntries = true)
    public void delete(Long id) {
        log.debug("Request to delete Partner: {}", id);
        partnerRepository.deleteById(id);
        auditLogger.log("PARTNER_DELETED", "Partner", String.valueOf(id), null);
    }

    // ---- Uniqueness checks ----

    @Transactional(readOnly = true)
    public boolean existsByClientShortName(String clientShortName) {
        return partnerRepository.existsByClientShortName(clientShortName);
    }

    @Transactional(readOnly = true)
    public boolean existsByAgentId(String agentId) {
        return partnerRepository.existsByAgentId(agentId);
    }

    @Transactional(readOnly = true)
    public boolean existsByContactEmail(String contactEmail) {
        return partnerRepository.existsByContactEmail(contactEmail);
    }

    @Transactional(readOnly = true)
    public boolean existsByContactPhone(String contactPhone) {
        return partnerRepository.existsByContactPhone(contactPhone);
    }

    public String getPartnerHash1(Long partnerId) {
        return partnerRepository.findById(partnerId)
            .map(Partner::getHash1)
            .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + partnerId));
    }

    private String generatePartnerCode() {
        return "PTR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String getCurrentUsername() {
        return PartnerAuditLogger.currentUsername();
    }
}
