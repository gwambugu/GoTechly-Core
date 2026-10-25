package com.dotechly.gotechly.core.service.partner;

import com.dotechly.gotechly.core.domain.PartnerAccount;
import com.dotechly.gotechly.core.repository.PartnerAccountRepository;
import com.dotechly.gotechly.core.service.dto.PartnerAccountDTO;
import com.dotechly.gotechly.core.service.mapper.PartnerAccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PartnerAccountService {

    private final Logger log = LoggerFactory.getLogger(PartnerAccountService.class);

    private final PartnerAccountRepository repository;
    private final PartnerAccountMapper mapper;
    private final PartnerManagementService partnerManagementService;
    private final IntegrityHashService integrityHashService;
    private final PartnerAuditLogger auditLogger;

    public PartnerAccountService(PartnerAccountRepository repository, PartnerAccountMapper mapper,
                                 PartnerManagementService partnerManagementService,
                                 IntegrityHashService integrityHashService,
                                 PartnerAuditLogger auditLogger) {
        this.repository = repository;
        this.mapper = mapper;
        this.partnerManagementService = partnerManagementService;
        this.integrityHashService = integrityHashService;
        this.auditLogger = auditLogger;
    }

    public PartnerAccountDTO save(PartnerAccountDTO dto) {
        log.debug("Request to save PartnerAccount : {}", dto);
        String username = PartnerAuditLogger.currentUsername();
        boolean isNew = (dto.getId() == null);
        PartnerAccount entity = mapper.toEntity(dto);
        if (entity.getStatus() == null || entity.getStatus().isBlank()) {
            entity.setStatus("ACTIVE");
        }
        if (entity.getIsPrimary() == null) {
            entity.setIsPrimary(false);
        }
        if (isNew) {
            entity.setDateCreated(Instant.now());
            entity.setCreatedBy(username);
            entity.setMakerId(username);
            entity.setMakerDate(Instant.now());
        } else {
            entity.setDateModified(Instant.now());
            entity.setModifiedBy(username);
        }

        // Compute HMAC integrity hash on both create and update
        String partnerHash = partnerManagementService.getPartnerHash1(entity.getPartnerId());
        entity.setHash1(integrityHashService.computeAccountHash(
            partnerHash, entity.getAccountNumber(), entity.getAccountType(),
            entity.getBankCode(), entity.getCurrencyCode(), entity.getIsPrimary()
        ));

        entity = repository.save(entity);
        auditLogger.log(isNew ? "PARTNER_ACCOUNT_CREATED" : "PARTNER_ACCOUNT_UPDATED",
            "PartnerAccount", String.valueOf(entity.getId()),
            "partnerId=" + entity.getPartnerId() + ", accountNumber=" + entity.getAccountNumber());
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<PartnerAccountDTO> findByPartnerId(Long partnerId) {
        List<PartnerAccount> accounts = repository.findByPartnerId(partnerId);
        String partnerHash = partnerManagementService.getPartnerHash1(partnerId);
        for (PartnerAccount account : accounts) {
            verifyIntegrity(account, partnerHash);
        }
        return mapper.toDto(accounts);
    }

    @Transactional(readOnly = true)
    public Optional<PartnerAccountDTO> findOne(Long id) {
        return repository.findById(id).map(account -> {
            String partnerHash = partnerManagementService.getPartnerHash1(account.getPartnerId());
            verifyIntegrity(account, partnerHash);
            return mapper.toDto(account);
        });
    }

    public void delete(Long id) {
        log.debug("Request to delete PartnerAccount : {}", id);
        repository.deleteById(id);
        auditLogger.log("PARTNER_ACCOUNT_DELETED", "PartnerAccount", String.valueOf(id), null);
    }

    @Transactional(readOnly = true)
    public boolean existsByAccountNumber(String accountNumber) {
        return repository.existsByAccountNumber(accountNumber);
    }

    private void verifyIntegrity(PartnerAccount account, String partnerHash) {
        if (account.getHash1() == null) {
            log.warn("PartnerAccount {} has no integrity hash — skipping verification (legacy record)", account.getId());
            return;
        }

        String computedHash = integrityHashService.computeAccountHash(
            partnerHash, account.getAccountNumber(), account.getAccountType(),
            account.getBankCode(), account.getCurrencyCode(), account.getIsPrimary()
        );

        if (!integrityHashService.verify(account.getHash1(), computedHash)) {
            log.error("INTEGRITY CHECK FAILED for partner account {} (accountNumber: {})",
                account.getId(), account.getAccountNumber());
            throw new IllegalStateException("Data integrity check failed for partner account: " + account.getId());
        }
    }
}
