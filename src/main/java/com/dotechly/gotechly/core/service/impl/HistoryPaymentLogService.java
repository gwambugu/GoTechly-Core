package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.domain.HistoryPaymentLog;
import com.dotechly.gotechly.core.repository.HistoryPaymentLogRepository;
import com.dotechly.gotechly.core.service.dto.HistoryPaymentLogDTO;
import com.dotechly.gotechly.core.service.mapper.HistoryPaymentLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class HistoryPaymentLogService {

    private final Logger log = LoggerFactory.getLogger(HistoryPaymentLogService.class);
    private final HistoryPaymentLogRepository historyPaymentLogRepository;
    private final HistoryPaymentLogMapper historyPaymentLogMapper;

    public HistoryPaymentLogService(HistoryPaymentLogRepository historyPaymentLogRepository, HistoryPaymentLogMapper historyPaymentLogMapper) {
        this.historyPaymentLogRepository = historyPaymentLogRepository;
        this.historyPaymentLogMapper = historyPaymentLogMapper;
    }

    public HistoryPaymentLogDTO save(HistoryPaymentLogDTO dto) {
        log.debug("Request to save HistoryPaymentLog : {}", dto);
        HistoryPaymentLog entity = historyPaymentLogMapper.toEntity(dto);
        entity = historyPaymentLogRepository.save(entity);
        return historyPaymentLogMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<HistoryPaymentLogDTO> findAll(Pageable pageable) {
        log.debug("Request to get all HistoryPaymentLogs");
        return historyPaymentLogRepository.findAll(pageable).map(historyPaymentLogMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<HistoryPaymentLogDTO> findOne(Long id) {
        log.debug("Request to get HistoryPaymentLog : {}", id);
        return historyPaymentLogRepository.findById(id).map(historyPaymentLogMapper::toDto);
    }

    public void delete(Long id) {
        log.debug("Request to delete HistoryPaymentLog : {}", id);
        historyPaymentLogRepository.deleteById(id);
    }
}
