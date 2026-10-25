package com.dotechly.gotechly.core.service.partner;

import com.dotechly.gotechly.core.domain.PartnerServiceEntity;
import com.dotechly.gotechly.core.repository.PartnerServiceRepository;
import com.dotechly.gotechly.core.service.dto.PartnerServiceDTO;
import com.dotechly.gotechly.core.service.mapper.PartnerServiceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PartnerServiceManagementService {

    private final Logger log = LoggerFactory.getLogger(PartnerServiceManagementService.class);

    private final PartnerServiceRepository repository;
    private final PartnerServiceMapper mapper;
    private final PartnerAuditLogger auditLogger;

    public PartnerServiceManagementService(PartnerServiceRepository repository, PartnerServiceMapper mapper,
                                           PartnerAuditLogger auditLogger) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditLogger = auditLogger;
    }

    public PartnerServiceDTO save(PartnerServiceDTO dto) {
        log.debug("Request to save PartnerService : {}", dto);
        String username = PartnerAuditLogger.currentUsername();
        boolean isNew = (dto.getId() == null);
        PartnerServiceEntity entity = mapper.toEntity(dto);
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
        auditLogger.log(isNew ? "PARTNER_SERVICE_CREATED" : "PARTNER_SERVICE_UPDATED",
            "PartnerService", String.valueOf(entity.getId()),
            "partnerId=" + entity.getPartnerId() + ", paymentType=" + entity.getPaymentType()
                + ", direction=" + entity.getDirection());
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<PartnerServiceDTO> findByPartnerId(Long partnerId) {
        return mapper.toDto(repository.findByPartnerId(partnerId));
    }

    @Transactional(readOnly = true)
    public Optional<PartnerServiceDTO> findOne(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    public void delete(Long id) {
        log.debug("Request to delete PartnerService : {}", id);
        repository.deleteById(id);
        auditLogger.log("PARTNER_SERVICE_DELETED", "PartnerService", String.valueOf(id), null);
    }
}
