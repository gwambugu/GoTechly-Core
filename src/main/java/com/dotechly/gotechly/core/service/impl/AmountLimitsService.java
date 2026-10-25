package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.domain.AmountLimits;
import com.dotechly.gotechly.core.repository.AmountLimitsRepository;
import com.dotechly.gotechly.core.service.dto.AmountLimitsDTO;
import com.dotechly.gotechly.core.service.mapper.AmountLimitsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class AmountLimitsService {

    private final Logger log = LoggerFactory.getLogger(AmountLimitsService.class);

    private final AmountLimitsRepository amountLimitsRepository;

    private final AmountLimitsMapper amountLimitsMapper;

    public AmountLimitsService(AmountLimitsRepository amountLimitsRepository, AmountLimitsMapper amountLimitsMapper) {
        this.amountLimitsRepository = amountLimitsRepository;
        this.amountLimitsMapper = amountLimitsMapper;
    }

    public AmountLimitsDTO save(AmountLimitsDTO amountLimitsDTO) {
        log.debug("Request to save AmountLimits : {}", amountLimitsDTO);
        AmountLimits amountLimits = amountLimitsMapper.toEntity(amountLimitsDTO);
        amountLimits = amountLimitsRepository.save(amountLimits);
        return amountLimitsMapper.toDto(amountLimits);
    }

    @Transactional(readOnly = true)
    public Page<AmountLimitsDTO> findAll(Pageable pageable) {
        log.debug("Request to get all AmountLimits");
        return amountLimitsRepository.findAll(pageable).map(amountLimitsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<AmountLimitsDTO> findOne(Long id) {
        log.debug("Request to get AmountLimits : {}", id);
        return amountLimitsRepository.findById(id).map(amountLimitsMapper::toDto);
    }

    public void delete(Long id) {
        log.debug("Request to delete AmountLimits : {}", id);
        amountLimitsRepository.deleteById(id);
    }
}
