package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.config.CacheConfiguration;
import com.dotechly.gotechly.core.domain.ErrorCodes;
import com.dotechly.gotechly.core.repository.ErrorCodesRepository;
import com.dotechly.gotechly.core.service.dto.ErrorCodesDTO;
import com.dotechly.gotechly.core.service.mapper.ErrorCodesMapper;
import com.dotechly.gotechly.spi.service.ResponseCodeLookup;
import com.dotechly.gotechly.spi.service.enumeration.ResponseCodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
@Transactional
public class ErrorCodesService implements ResponseCodeLookup {

    private final Logger log = LoggerFactory.getLogger(ErrorCodesService.class);

    private final ErrorCodesRepository errorCodesRepository;

    private final ErrorCodesMapper errorCodesMapper;

    public ErrorCodesService(ErrorCodesRepository errorCodesRepository, ErrorCodesMapper errorCodesMapper) {
        this.errorCodesRepository = errorCodesRepository;
        this.errorCodesMapper = errorCodesMapper;
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfiguration.CACHE_ERROR_CODES, allEntries = true),
            @CacheEvict(value = CacheConfiguration.CACHE_ERROR_CODES_BY_API, allEntries = true),
            @CacheEvict(value = CacheConfiguration.CACHE_ERROR_CODE_DESCRIPTIONS, allEntries = true)
    })
    public ErrorCodesDTO save(ErrorCodesDTO errorCodesDTO) {
        log.debug("Request to save ErrorCodes : {}", errorCodesDTO);
        ErrorCodes errorCodes = errorCodesMapper.toEntity(errorCodesDTO);
        errorCodes = errorCodesRepository.save(errorCodes);
        return errorCodesMapper.toDto(errorCodes);
    }

    @Transactional(readOnly = true)
    public Page<ErrorCodesDTO> findAll(Pageable pageable) {
        log.debug("Request to get all ErrorCodes");
        return errorCodesRepository.findAll(pageable).map(errorCodesMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<ErrorCodesDTO> findOne(Long id) {
        log.debug("Request to get ErrorCodes : {}", id);
        return errorCodesRepository.findById(id).map(errorCodesMapper::toDto);
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfiguration.CACHE_ERROR_CODES, allEntries = true),
            @CacheEvict(value = CacheConfiguration.CACHE_ERROR_CODES_BY_API, allEntries = true),
            @CacheEvict(value = CacheConfiguration.CACHE_ERROR_CODE_DESCRIPTIONS, allEntries = true)
    })
    public void delete(Long id) {
        log.debug("Request to delete ErrorCodes : {}", id);
        errorCodesRepository.deleteById(id);
    }

    @Cacheable(value = CacheConfiguration.CACHE_ERROR_CODES, key = "#serviceName + ':' + #errorCode")
    public ResponseCodeStatus getErrorState(String serviceName, String errorCode) {
        log.debug("Request to getTransactionState  : {} {}", serviceName,errorCode);
        Optional<ErrorCodes> errorCodesOptional = errorCodesRepository.findByServiceNameAndErrorCode(serviceName,errorCode);
        if(errorCodesOptional.isPresent()){
            log.debug("Error code Mapped in errorCode table ");
            ErrorCodes errorCodes = errorCodesOptional.orElseThrow();
            return  enumFromString(errorCodes.getTransactionState());
        }else{
            log.debug("Error code NOT Mapped in errorCode table ");
            return ResponseCodeStatus.PENDING;
        }
    }

    @Cacheable(value = CacheConfiguration.CACHE_ERROR_CODES_BY_API, key = "#apiName")
    @Transactional(readOnly = true)
    public Map<String, ResponseCodeStatus> findAllByApiName(String apiName) {
        log.debug("Request to bulk-fetch ErrorCodes for apiName : {}", apiName);
        if (apiName == null || apiName.isBlank()) {
            return Collections.emptyMap();
        }
        List<ErrorCodes> rows = errorCodesRepository.findByApiName(apiName);
        if (rows.isEmpty()) {
            log.warn("No error codes found for apiName='{}'. Plugin will treat all responses as PENDING.", apiName);
            return Collections.emptyMap();
        }
        Map<String, ResponseCodeStatus> out = new LinkedHashMap<>(rows.size());
        for (ErrorCodes row : rows) {
            if (row.getErrorCode() == null) continue;
            out.put(row.getErrorCode(), enumFromString(row.getTransactionState()));
        }
        log.info("Loaded {} error-code mappings for apiName='{}'", out.size(), apiName);
        return Collections.unmodifiableMap(out);
    }

    /**
     * Bulk lookup of {@code error_code → error_description} for an {@code api_name}.
     *
     * <p>Companion to {@link #findAllByApiName(String)}, which carries the transaction state but
     * drops the description. The channel-facing response needs the description so
     * {@code responseDesc} reads as a sentence rather than a restated code; keeping it as a separate
     * cached view avoids changing the {@code ResponseCodeLookup} SPI contract that plugins compile
     * against.
     *
     * @param apiName conventionally {@code {paymentType}.{serviceScope}}, e.g. {@code RNDPS.OUT_PAYMENT}
     * @return immutable map of error code → description; empty when no rows, or for rows with no
     *         description set
     */
    @Cacheable(value = CacheConfiguration.CACHE_ERROR_CODE_DESCRIPTIONS, key = "#apiName")
    @Transactional(readOnly = true)
    public Map<String, String> findDescriptionsByApiName(String apiName) {
        log.debug("Request to bulk-fetch error-code descriptions for apiName : {}", apiName);
        if (apiName == null || apiName.isBlank()) {
            return Collections.emptyMap();
        }
        List<ErrorCodes> rows = errorCodesRepository.findByApiName(apiName);
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> out = new LinkedHashMap<>(rows.size());
        for (ErrorCodes row : rows) {
            if (row.getErrorCode() == null) continue;
            if (row.getErrorDescription() == null || row.getErrorDescription().isBlank()) continue;
            out.put(row.getErrorCode(), row.getErrorDescription());
        }
        return Collections.unmodifiableMap(out);
    }

    public ResponseCodeStatus enumFromString(String value) {
        if (value == null) return ResponseCodeStatus.PENDING;
        try {
            return ResponseCodeStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return ResponseCodeStatus.PENDING;
        }
    }
}
