package com.dotechly.gotechly.core.service.impl;


import com.dotechly.gotechly.core.domain.LinkedWalletSubscription;
import com.dotechly.gotechly.core.repository.LinkedWalletSubscriptionRepository;
import com.dotechly.gotechly.core.service.dto.LinkedWalletSubscriptionDTO;
import com.dotechly.gotechly.core.service.mapper.LinkedWalletSubscriptionMapper;
import com.dotechly.gotechly.spi.service.inward.linkedwallet.LinkedWalletProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class LinkedWalletSubscriptionService implements LinkedWalletProfile {

    private final Logger log = LoggerFactory.getLogger(LinkedWalletSubscriptionService.class);

    private final LinkedWalletSubscriptionRepository repository;
    private final LinkedWalletSubscriptionMapper mapper;

    public LinkedWalletSubscriptionService(LinkedWalletSubscriptionRepository repository,
                                           LinkedWalletSubscriptionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public LinkedWalletSubscriptionDTO save(LinkedWalletSubscriptionDTO dto) {
        log.debug("Request to save LinkedWalletSubscription : {}", dto);
        LinkedWalletSubscription entity = mapper.toEntity(dto);
        entity = repository.save(entity);
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<LinkedWalletSubscriptionDTO> findAll(Pageable pageable) {
        log.debug("Request to get all LinkedWalletSubscription");
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<LinkedWalletSubscriptionDTO> findOne(Long id) {
        log.debug("Request to get LinkedWalletSubscription : {}", id);
        return repository.findById(id).map(mapper::toDto);
    }

    public void delete(Long id) {
        log.debug("Request to delete LinkedWalletSubscription : {}", id);
        repository.deleteById(id);
    }
    @Override
    public Boolean checkIfCustomerIsLinked(String savingsAccountNumber,
                                 String mobilePhoneNumber,
                                 Long vmtRefId){
        Optional<LinkedWalletSubscription> linkedWalletSubscriptionOptional = repository.findFirstBySavingsAccountNumberAndMobilePhoneNumberAndVmtRefId(savingsAccountNumber,mobilePhoneNumber,vmtRefId);
        return linkedWalletSubscriptionOptional.isPresent();
    }
}
