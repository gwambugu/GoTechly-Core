package com.dotechly.gotechly.core.service.partner;

import com.dotechly.gotechly.core.domain.PartnerTransactionLimit;
import com.dotechly.gotechly.core.repository.PartnerTransactionLimitRepository;
import com.dotechly.gotechly.core.service.dto.PartnerTransactionLimitDTO;
import com.dotechly.gotechly.core.service.mapper.PartnerTransactionLimitMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PartnerTransactionLimitService {

    private final Logger log = LoggerFactory.getLogger(PartnerTransactionLimitService.class);

    private final PartnerTransactionLimitRepository repository;
    private final PartnerTransactionLimitMapper mapper;
    private final PartnerAuditLogger auditLogger;

    public PartnerTransactionLimitService(PartnerTransactionLimitRepository repository,
                                          PartnerTransactionLimitMapper mapper,
                                          PartnerAuditLogger auditLogger) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditLogger = auditLogger;
    }

    public PartnerTransactionLimitDTO save(PartnerTransactionLimitDTO dto) {
        log.debug("Request to save PartnerTransactionLimit : {}", dto);
        String username = PartnerAuditLogger.currentUsername();
        boolean isNew = (dto.getId() == null);
        PartnerTransactionLimit entity = mapper.toEntity(dto);
        if (entity.getStatus() == null || entity.getStatus().isBlank()) {
            entity.setStatus("ACTIVE");
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
        entity = repository.save(entity);
        auditLogger.log(isNew ? "PARTNER_LIMIT_CREATED" : "PARTNER_LIMIT_UPDATED",
            "PartnerTransactionLimit", String.valueOf(entity.getId()),
            "partnerId=" + entity.getPartnerId() + ", paymentType=" + entity.getPaymentType()
                + ", currency=" + entity.getCurrencyCode());
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<PartnerTransactionLimitDTO> findByPartnerId(Long partnerId) {
        return mapper.toDto(repository.findByPartnerId(partnerId));
    }

    @Transactional(readOnly = true)
    public Optional<PartnerTransactionLimitDTO> findOne(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    public void delete(Long id) {
        log.debug("Request to delete PartnerTransactionLimit : {}", id);
        repository.deleteById(id);
        auditLogger.log("PARTNER_LIMIT_DELETED", "PartnerTransactionLimit", String.valueOf(id), null);
    }
}
