package com.dotechly.gotechly.core.service.partner;

import com.dotechly.gotechly.core.domain.PartnerConfig;
import com.dotechly.gotechly.core.repository.PartnerConfigRepository;
import com.dotechly.gotechly.core.service.dto.PartnerConfigDTO;
import com.dotechly.gotechly.core.service.mapper.PartnerConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PartnerConfigService {

    private final Logger log = LoggerFactory.getLogger(PartnerConfigService.class);

    private final PartnerConfigRepository repository;
    private final PartnerConfigMapper mapper;
    private final PartnerAuditLogger auditLogger;

    public PartnerConfigService(PartnerConfigRepository repository, PartnerConfigMapper mapper,
                                PartnerAuditLogger auditLogger) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditLogger = auditLogger;
    }

    public PartnerConfigDTO save(PartnerConfigDTO dto) {
        log.debug("Request to save PartnerConfig : {}", dto);
        String username = PartnerAuditLogger.currentUsername();
        boolean isNew = (dto.getId() == null);
        PartnerConfig entity = mapper.toEntity(dto);
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
        auditLogger.log(isNew ? "PARTNER_CONFIG_CREATED" : "PARTNER_CONFIG_UPDATED",
            "PartnerConfig", String.valueOf(entity.getId()),
            "partnerId=" + entity.getPartnerId() + ", paramName=" + entity.getParamName());
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<PartnerConfigDTO> findByPartnerId(Long partnerId) {
        return mapper.toDto(repository.findByPartnerId(partnerId));
    }

    @Transactional(readOnly = true)
    public Optional<PartnerConfigDTO> findOne(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    public void delete(Long id) {
        log.debug("Request to delete PartnerConfig : {}", id);
        repository.deleteById(id);
        auditLogger.log("PARTNER_CONFIG_DELETED", "PartnerConfig", String.valueOf(id), null);
    }
}
