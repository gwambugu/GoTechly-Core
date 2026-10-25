package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.domain.PendingPaymentLog;
import com.dotechly.gotechly.core.repository.PendingPaymentLogRepository;
import com.dotechly.gotechly.core.service.dto.PendingPaymentLogDTO;
import com.dotechly.gotechly.core.service.mapper.PendingPaymentLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class PendingPaymentLogService {

    private final Logger log = LoggerFactory.getLogger(PendingPaymentLogService.class);
    private final PendingPaymentLogRepository pendingPaymentLogRepository;
    private final PendingPaymentLogMapper pendingPaymentLogMapper;

    public PendingPaymentLogService(PendingPaymentLogRepository pendingPaymentLogRepository, PendingPaymentLogMapper pendingPaymentLogMapper) {
        this.pendingPaymentLogRepository = pendingPaymentLogRepository;
        this.pendingPaymentLogMapper = pendingPaymentLogMapper;
    }

    public PendingPaymentLogDTO save(PendingPaymentLogDTO dto) {
        log.debug("Request to save PendingPaymentLog : {}", dto);
        PendingPaymentLog entity = pendingPaymentLogMapper.toEntity(dto);
        entity = pendingPaymentLogRepository.save(entity);
        return pendingPaymentLogMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<PendingPaymentLogDTO> findAll(Pageable pageable) {
        log.debug("Request to get all PendingPaymentLogs");
        return pendingPaymentLogRepository.findAll(pageable).map(pendingPaymentLogMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<PendingPaymentLogDTO> findOne(Long id) {
        log.debug("Request to get PendingPaymentLog : {}", id);
        return pendingPaymentLogRepository.findById(id).map(pendingPaymentLogMapper::toDto);
    }

    public void delete(Long id) {
        log.debug("Request to delete PendingPaymentLog : {}", id);
        pendingPaymentLogRepository.deleteById(id);
    }
}
