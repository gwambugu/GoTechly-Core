package com.dotechly.gotechly.core.service.impl;


import com.dotechly.gotechly.core.domain.TemplateLookup;
import com.dotechly.gotechly.core.repository.TemplateLookupRepository;
import com.dotechly.gotechly.core.service.dto.TemplateLookupDTO;
import com.dotechly.gotechly.core.service.mapper.TemplateLookupMapper;
import com.dotechly.gotechly.spi.service.TemplateLookupSPIService;
import com.dotechly.gotechly.spi.service.dto.db.TemplateLookupSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class TemplateLookupService implements TemplateLookupSPIService {

    private final Logger log = LoggerFactory.getLogger(TemplateLookupService.class);

    private final TemplateLookupRepository repository;
    private final TemplateLookupMapper mapper;

    public TemplateLookupService(TemplateLookupRepository repository, TemplateLookupMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public TemplateLookupDTO save(TemplateLookupDTO dto) {
        log.debug("Request to save TemplateLookup : {}", dto);
        TemplateLookup entity = mapper.toEntity(dto);
        // Stamp modifyDate on every write so audit/maker-checker captures who last touched the row.
        entity.setModifyDate(Instant.now());
        entity = repository.save(entity);
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<TemplateLookupDTO> findAll(Pageable pageable) {
        log.debug("Request to get all TemplateLookups");
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<TemplateLookupDTO> findOne(Long id) {
        log.debug("Request to get TemplateLookup : {}", id);
        return repository.findById(id).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<TemplateLookupDTO> findByTemplateName(String templateName) {
        return repository.findByTemplateName(templateName).map(mapper::toDto);
    }

    public void delete(Long id) {
        log.debug("Request to delete TemplateLookup : {}", id);
        repository.deleteById(id);
    }

    @Override
    public Optional<TemplateLookupSPI> findByTemplateNameSPI(String templateName) {
        Optional<TemplateLookupSPI>   templateLookupSPI = repository.findByTemplateName(templateName).map(mapper::toSPI);
        return templateLookupSPI;
    }


}
