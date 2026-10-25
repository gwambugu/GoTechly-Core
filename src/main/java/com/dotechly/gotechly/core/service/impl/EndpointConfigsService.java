package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.config.CacheConfiguration;
import com.dotechly.gotechly.core.domain.EndpointConfigs;
import com.dotechly.gotechly.core.repository.EndpointConfigsRepository;
import com.dotechly.gotechly.core.service.dto.EndpointConfigsDTO;
import com.dotechly.gotechly.core.service.mapper.EndpointConfigsMapper;
import com.dotechly.gotechly.spi.service.EndPointConfigsLookup;
import com.dotechly.gotechly.spi.service.dto.db.EndpointConfigsSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class EndpointConfigsService implements EndPointConfigsLookup {

    private final Logger log = LoggerFactory.getLogger(EndpointConfigsService.class);

    private final EndpointConfigsRepository endpointConfigsRepository;

    private final EndpointConfigsMapper endpointConfigsMapper;

    public EndpointConfigsService(EndpointConfigsRepository endpointConfigsRepository, EndpointConfigsMapper endpointConfigsMapper) {
        this.endpointConfigsRepository = endpointConfigsRepository;
        this.endpointConfigsMapper = endpointConfigsMapper;
    }

    @CacheEvict(value = CacheConfiguration.CACHE_ENDPOINT_CONFIGS, allEntries = true)
    public EndpointConfigsDTO save(EndpointConfigsDTO endpointConfigsDTO) {
        log.debug("Request to save EndpointConfigs : {}", endpointConfigsDTO);
        EndpointConfigs endpointConfigs = endpointConfigsMapper.toEntity(endpointConfigsDTO);
        endpointConfigs = endpointConfigsRepository.save(endpointConfigs);
        return endpointConfigsMapper.toDto(endpointConfigs);
    }

    @Transactional(readOnly = true)
    public Page<EndpointConfigsDTO> findAll(Pageable pageable) {
        log.debug("Request to get all EndpointConfigs");
        return endpointConfigsRepository.findAll(pageable).map(endpointConfigsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<EndpointConfigsDTO> findOne(Long id) {
        log.debug("Request to get EndpointConfigs : {}", id);
        return endpointConfigsRepository.findById(id).map(endpointConfigsMapper::toDto);
    }

    @CacheEvict(value = CacheConfiguration.CACHE_ENDPOINT_CONFIGS, allEntries = true)
    public void delete(Long id) {
        log.debug("Request to delete EndpointConfigs : {}", id);
        endpointConfigsRepository.deleteById(id);
    }

    @Cacheable(value = CacheConfiguration.CACHE_ENDPOINT_CONFIGS, key = "#serviceName")
    @Override
    public Optional<EndpointConfigsSPI> findEndpointConfigsByServiceName(String serviceName) {
        log.debug("Request to get EndpointConfigs : {}", serviceName);
        return endpointConfigsRepository.findByServiceName(serviceName).map(this::toSpi);
    }
    private EndpointConfigsSPI toSpi(EndpointConfigs endpointConfigs){
        EndpointConfigsSPI spi = new EndpointConfigsSPI();
        BeanUtils.copyProperties(endpointConfigs, spi);
        return spi;
    }
}
