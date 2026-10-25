package com.dotechly.gotechly.core.service.impl;


import com.dotechly.gotechly.core.domain.BankCodesLookup;
import com.dotechly.gotechly.core.repository.BankCodesLookupRepository;
import com.dotechly.gotechly.core.service.dto.BankCodesLookupDTO;
import com.dotechly.gotechly.core.service.mapper.BankCodesLookupMapper;

import com.dotechly.gotechly.spi.service.BankCodeLookupSPIService;
import com.dotechly.gotechly.spi.service.dto.db.BankCodesLookupSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class BankCodesLookupService implements BankCodeLookupSPIService {

    private final Logger log = LoggerFactory.getLogger(BankCodesLookupService.class);

    private final BankCodesLookupRepository repository;
    private final BankCodesLookupMapper mapper;

    public BankCodesLookupService(BankCodesLookupRepository repository, BankCodesLookupMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public BankCodesLookupDTO save(BankCodesLookupDTO dto) {
        log.debug("Request to save BankCodesLookup : {}", dto);
        BankCodesLookup entity = mapper.toEntity(dto);
        entity = repository.save(entity);
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<BankCodesLookupDTO> findAll(Pageable pageable) {
        log.debug("Request to get all BankCodesLookup");
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<BankCodesLookupDTO> findOne(Long id) {
        log.debug("Request to get BankCodesLookup : {}", id);
        return repository.findById(id).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<BankCodesLookupDTO> findByBic(String bic) {
        return repository.findFirstByBic(bic).map(mapper::toDto);
    }
    @Transactional(readOnly = true)
    public Optional<BankCodesLookupDTO> findByBankCode(String bankCode) {
        return repository.findFirstByBankCode(bankCode).map(mapper::toDto);
    }
    @Transactional(readOnly = true)
    public Optional<BankCodesLookupDTO> findByPic(String pic) {
        return repository.findFirstByPic(pic).map(mapper::toDto);
    }

    public void delete(Long id) {
        log.debug("Request to delete BankCodesLookup : {}", id);
        repository.deleteById(id);
    }

    @Override
    public Optional<BankCodesLookupSPI> findByBicSPI(String bic) {
        return repository.findFirstByBic(bic).map(mapper::toSPI);
    }

    @Override
    public Optional<BankCodesLookupSPI> findByBicStartingWithSPI(String bic) {
        return repository.findFirstByBicStartingWith(bic).map(mapper::toSPI);
    }

    @Override
    public Optional<BankCodesLookupSPI> findByBankCodeSPI(String bic) {
        return repository.findFirstByBankCode(bic).map(mapper::toSPI);
    }

    @Override
    public Optional<BankCodesLookupSPI> findByPicSPI(String bic) {
        return repository.findFirstByPic(bic).map(mapper::toSPI);
    }
}
