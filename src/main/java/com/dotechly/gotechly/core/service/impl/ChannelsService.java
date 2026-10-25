package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.config.CacheConfiguration;
import com.dotechly.gotechly.core.domain.Channels;
import com.dotechly.gotechly.core.repository.ChannelsRepository;
import com.dotechly.gotechly.core.service.dto.ChannelsDTO;
import com.dotechly.gotechly.core.service.mapper.ChannelsMapper;
import com.dotechly.gotechly.spi.service.ChannelsSPIService;
import com.dotechly.gotechly.spi.service.dto.db.ChannelsSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class ChannelsService implements ChannelsSPIService {

    private final Logger log = LoggerFactory.getLogger(ChannelsService.class);

    private final ChannelsRepository channelsRepository;

    private final ChannelsMapper channelsMapper;

    public ChannelsService(ChannelsRepository channelsRepository, ChannelsMapper channelsMapper) {
        this.channelsRepository = channelsRepository;
        this.channelsMapper = channelsMapper;
    }

    @CacheEvict(value = CacheConfiguration.CACHE_CHANNELS, allEntries = true)
    public ChannelsDTO save(ChannelsDTO channelsDTO) {
        log.debug("Request to save Channels : {}", channelsDTO);
        Channels channels = channelsMapper.toEntity(channelsDTO);
        channels = channelsRepository.save(channels);
        return channelsMapper.toDto(channels);
    }

    @Transactional(readOnly = true)
    public Page<ChannelsDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Channels");
        return channelsRepository.findAll(pageable).map(channelsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<ChannelsDTO> findOne(Long id) {
        log.debug("Request to get Channels : {}", id);
        return channelsRepository.findById(id).map(channelsMapper::toDto);
    }

    @Cacheable(value = CacheConfiguration.CACHE_CHANNELS, key = "#systemName + ':' + #apiName")
    @Transactional(readOnly = true)
    public Optional<Channels> findBySystemNameAndApiName(String systemName, String apiName) {
        log.debug("Request to find Channel by systemName='{}' and apiName='{}'", systemName, apiName);
        return channelsRepository.findBySystemNameAndApiName(systemName, apiName);
    }
    @Cacheable(value = CacheConfiguration.CACHE_CHANNELS, key = "#systemName + ':' + #apiName")
    @Transactional(readOnly = true)
    public Optional<Channels> findBySourceSystem(String sourceSystem) {
        log.debug("Request to find Channel by sourceSystem='{}''",sourceSystem);
        return channelsRepository.findByChannelName(sourceSystem);
    }

    @CacheEvict(value = CacheConfiguration.CACHE_CHANNELS, allEntries = true)
    public void delete(Long id) {
        log.debug("Request to delete Channels : {}", id);
        channelsRepository.deleteById(id);
    }

    @Override
    public Optional<ChannelsSPI> findBySourceSystemSPI(String sourceSystem) {
        return channelsRepository.findByChannelName(sourceSystem).map(channelsMapper::toSPI);
    }
}
