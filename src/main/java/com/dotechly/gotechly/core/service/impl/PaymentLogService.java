package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.domain.PaymentLog;
import com.dotechly.gotechly.core.repository.PaymentLogRepository;
import com.dotechly.gotechly.core.service.dto.PaymentLogDTO;
import com.dotechly.gotechly.core.service.mapper.PaymentLogMapper;
import com.dotechly.gotechly.core.util.PaymentSpiConverter;
import com.dotechly.gotechly.spi.service.CachedReceiverNamesLookup;
import com.dotechly.gotechly.spi.service.PaymentLogLookup;
import com.dotechly.gotechly.spi.service.dto.db.CachedReceiverNames;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class PaymentLogService implements PaymentLogLookup, CachedReceiverNamesLookup {

    private final Logger log = LoggerFactory.getLogger(PaymentLogService.class);

    private final PaymentLogRepository paymentLogRepository;

    private final PaymentLogMapper paymentLogMapper;
    private final PaymentSpiConverter paymentSpiConverter;

    public PaymentLogService(PaymentLogRepository paymentLogRepository, PaymentLogMapper paymentLogMapper,
                             PaymentSpiConverter paymentSpiConverter) {
        this.paymentLogRepository = paymentLogRepository;
        this.paymentLogMapper = paymentLogMapper;
        this.paymentSpiConverter = paymentSpiConverter;
    }

    public PaymentLogDTO save(PaymentLogDTO paymentLogDTO) {
        log.info("Saving PaymentLog {}", paymentLogDTO.getRequestId());
        PaymentLog paymentLog = paymentLogMapper.toEntity(paymentLogDTO);
        paymentLog = paymentLogRepository.save(paymentLog);
        return paymentLogMapper.toDto(paymentLog);
    }

    @Transactional(readOnly = true)
    public Page<PaymentLogDTO> findAll(Pageable pageable) {
        log.debug("Request to get all PaymentLogs");
        return paymentLogRepository.findAll(pageable).map(paymentLogMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentLogDTO> findOne(Long id) {
        log.debug("Request to get PaymentLog : {}", id);
        return paymentLogRepository.findById(id).map(paymentLogMapper::toDto);
    }

    public void delete(Long id) {
        log.debug("Request to delete PaymentLog : {}", id);
        paymentLogRepository.deleteById(id);
    }

    @Override
    public Optional<PaymentLogSPI> findByOriginalRequestId(String requestId) {
        log.debug("Request to findByRequestId : {}", requestId);
        Optional<PaymentLogDTO> paymentLogDTOOptional = paymentLogRepository.findByRequestId(requestId).map(paymentLogMapper::toDto);
        if(paymentLogDTOOptional.isPresent()){
            PaymentLogSPI paymentLogSPI = paymentSpiConverter.toSpi(paymentLogDTOOptional.orElseThrow());
            return Optional.of(paymentLogSPI);
        }else{
            return Optional.empty();
        }

    }
    @Transactional(readOnly = true)
    public Optional<PaymentLogDTO> findFirstByRequestId(String requestId) {
        log.debug("Request to get PaymentLog : {}", requestId);
        return paymentLogRepository.findFirstByRequestId(requestId).map(paymentLogMapper::toDto);
    }
    public Optional<PaymentLogSPI> fetchOriginalRequest(String requestId) {
        log.debug("Request to findByRequestId : {}", requestId);
        Optional<PaymentLogDTO> paymentLogDTOOptional = paymentLogRepository.findByRequestId(requestId).map(paymentLogMapper::toDto);
        if(paymentLogDTOOptional.isPresent()){
            PaymentLogDTO paymentLogDTO = paymentLogDTOOptional.orElseThrow();
            Integer statusCount  =(paymentLogDTO.getStatusCount()==null)? 0 : paymentLogDTO.getStatusCount();
            statusCount++;
            paymentLogDTO.setStatusCount(statusCount);
            paymentLogDTO.setStatusTimestamp(Instant.now());
            paymentLogDTO = save(paymentLogDTO);
            PaymentLogSPI paymentLogSPI = paymentSpiConverter.toSpi(paymentLogDTO);
            return Optional.of(paymentLogSPI);
        }else{
            return Optional.empty();
        }

    }
    public Optional<PaymentLogSPI> saveNewRequest(PaymentLogSPI request) {
        log.info("saveNewRequest==> Checking if request exists {}", request.getDetails().getRequestId());
        Optional<PaymentLogDTO> paymentLogDTOOptional = paymentLogRepository.findByRequestIdInAllTables(request.getDetails().getRequestId()).map(paymentLogMapper::toDto);
        if(paymentLogDTOOptional.isEmpty()){
            log.info("SaveNewRequest==> saving now requestId {}", request.getDetails().getRequestId());
            PaymentLogDTO paymentLogDTO = paymentSpiConverter.toDto(request);
            if(paymentLogDTO.getRequestTimestamp()==null){
                paymentLogDTO.setRequestTimestamp(Instant.now());
            }
            Instant now = Instant.now();
            paymentLogDTO.setCreatedAt(now);
            paymentLogDTO.setUpdatedAt(now);
            paymentLogDTO = save(paymentLogDTO);
            PaymentLogSPI paymentLogSPI = paymentSpiConverter.toSpi(paymentLogDTO);
            return Optional.of(paymentLogSPI);
        }else{
            log.error("There is an existing record in DB with same request id: {}",request.getDetails().getRequestId());
            return Optional.empty();
        }

    }
    public Optional<PaymentLogSPI> findByRequestIdInAllTables(String requestId){
        Optional<PaymentLogDTO>  paymentLogDTOOp =  paymentLogRepository.findByRequestIdInAllTables(requestId).map(paymentLogMapper::toDto);
        if(paymentLogDTOOp.isEmpty()){
            return Optional.empty();
        }else{
            PaymentLogDTO paymentLogDTO  = paymentLogDTOOp.get();
            PaymentLogSPI paymentLogSPI = paymentSpiConverter.toSpi(paymentLogDTO);
            return Optional.of(paymentLogSPI);
        }
    }

    public Optional<PaymentLogSPI> saveNewRequest2(PaymentLogSPI request) {
        log.info("saveNewRequest==>  request {}", request.getDetails().getRequestId());
            log.info("SaveNewRequest2==> saving now requestId {}", request.getDetails().getRequestId());
            PaymentLogDTO paymentLogDTO = paymentSpiConverter.toDto(request);
            if(paymentLogDTO.getRequestTimestamp()==null){
                paymentLogDTO.setRequestTimestamp(Instant.now());
            }
            Instant now = Instant.now();
            paymentLogDTO.setCreatedAt(now);
            paymentLogDTO.setUpdatedAt(now);
            paymentLogDTO = save(paymentLogDTO);
            PaymentLogSPI paymentLogSPI = paymentSpiConverter.toSpi(paymentLogDTO);
            return Optional.of(paymentLogSPI);
    }

    public Optional<PaymentLogSPI> update(PaymentLogSPI request) {
        log.debug("Request to update request : {}", request.getDetails().getRequestId());
        PaymentLogDTO paymentLogDTO = paymentSpiConverter.toDto(request);
        paymentLogDTO = save(paymentLogDTO);
        PaymentLogSPI paymentLogSPI = paymentSpiConverter.toSpi(paymentLogDTO);
        return Optional.of(paymentLogSPI);
    }
    @Override
    public Optional<CachedReceiverNames> findCachedReceiverNameByMsisdn(String receiverMsisdn) {
        log.debug("Request to findCachedReceiverNameByMsisdnt : {}", receiverMsisdn);
        return paymentLogRepository.findLatestReceiverNameByMsisdn(receiverMsisdn);
    }

}
