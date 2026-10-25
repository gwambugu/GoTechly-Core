package com.dotechly.gotechly.core.service.impl;


import com.dotechly.gotechly.core.config.CacheConfiguration;
import com.dotechly.gotechly.core.domain.AppConfigs;
import com.dotechly.gotechly.core.repository.AppConfigsRepository;
import com.dotechly.gotechly.core.service.dto.AppConfigsDTO;
import com.dotechly.gotechly.core.service.mapper.AppConfigsMapper;
import com.dotechly.gotechly.spi.service.AppConfigsLookup;
import com.dotechly.gotechly.spi.service.dto.db.AppConfigsSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AppConfigsService implements AppConfigsLookup {

    private final Logger log = LoggerFactory.getLogger(AppConfigsService.class);

    private final AppConfigsRepository appConfigsRepository;

    private final AppConfigsMapper appConfigsMapper;

    private final AppConfigsCryptoService crypto;

    public AppConfigsService(AppConfigsRepository appConfigsRepository, AppConfigsMapper appConfigsMapper,
                             AppConfigsCryptoService crypto) {
        this.appConfigsRepository = appConfigsRepository;
        this.appConfigsMapper = appConfigsMapper;
        this.crypto = crypto;
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfiguration.CACHE_APP_CONFIGS, allEntries = true),
            @CacheEvict(value = CacheConfiguration.CACHE_APP_CONFIGS_BY_PREFIX, allEntries = true)
    })
    public AppConfigsDTO save(AppConfigsDTO appConfigsDTO) {
        log.debug("Request to save AppConfigs : {}", appConfigsDTO.getConfigId());
        // Encrypt-at-rest for secrets. encryptIfNeeded is idempotent (a value already
        // carrying the enc:v1: sentinel — e.g. one encrypted at submit time — passes
        // through untouched, so we never double-encrypt).
        if (appConfigsDTO.isSensitive()) {
            appConfigsDTO.setConfigValue(crypto.encryptIfNeeded(appConfigsDTO.getConfigValue()));
        }
        AppConfigs appConfigs = appConfigsMapper.toEntity(appConfigsDTO);
        appConfigs = appConfigsRepository.save(appConfigs);
        return appConfigsMapper.toDto(appConfigs);
    }

    /**
     * Raw stored {@code config_value} for a row, bypassing both mask and decryption.
     * Used only by the maker-checker apply path to preserve an existing secret when an
     * UPDATE leaves the value blank/masked. Not exposed over REST.
     */
    @Transactional(readOnly = true)
    public Optional<String> findRawConfigValue(Long id) {
        return appConfigsRepository.findById(id).map(AppConfigs::getConfigValue);
    }

    @Transactional(readOnly = true)
    public Page<AppConfigsDTO> findAll(Pageable pageable) {
        log.debug("Request to get all AppConfigs");
        return appConfigsRepository.findAll(pageable).map(appConfigsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<AppConfigsDTO> findOne(Long  id) {
        log.debug("Request to get AppConfigs : {}", id);
        return appConfigsRepository.findById(id).map(appConfigsMapper::toDto);
    }

    /**
     * Evicts both AppConfigs caches without touching the DB.
     * <p>
     * The cache manager is in-memory and per-JVM, so a {@link #save}/{@link #delete} on one
     * Core instance does not evict the cache on its peers. The plugin required-configs preflight
     * ({@code PluginVerifier.verifyRequiredConfigs}) and the per-plugin config build
     * ({@code PluginLoader.createContextForPlugin}) both read through {@code appConfigsByPrefix};
     * if a config was added on a different instance, the reloading instance can keep serving a
     * stale (empty) cached view, failing the redeploy with "missing required config" until restart.
     * {@code PluginLoader} calls this at the start of every (re)load so the load path reads
     * ground truth from the DB. Evicts the paired views together (see {@code appConfigs} +
     * {@code appConfigsByPrefix}).
     */
    @Caching(evict = {
            @CacheEvict(value = CacheConfiguration.CACHE_APP_CONFIGS, allEntries = true),
            @CacheEvict(value = CacheConfiguration.CACHE_APP_CONFIGS_BY_PREFIX, allEntries = true)
    })
    public void evictCaches() {
        log.debug("Evicting AppConfigs caches (appConfigs + appConfigsByPrefix) for fresh plugin-load read");
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfiguration.CACHE_APP_CONFIGS, allEntries = true),
            @CacheEvict(value = CacheConfiguration.CACHE_APP_CONFIGS_BY_PREFIX, allEntries = true)
    })
    public void delete(Long id) {
        log.debug("Request to delete AppConfigs : {}", id);
        appConfigsRepository.deleteById(id);
    }

    @Cacheable(value = CacheConfiguration.CACHE_APP_CONFIGS, key = "#configId")
    @Transactional(readOnly = true)
    public Optional<AppConfigsSPI> findByConfigId(String configId) {
        log.debug("Request to get AppConfigs : {}", configId);
        return appConfigsRepository.findByConfigId(configId).map(this::toSpi);
    }

    @Cacheable(value = CacheConfiguration.CACHE_APP_CONFIGS_BY_PREFIX, key = "#prefix")
    @Transactional(readOnly = true)
    public Map<String, String> findByConfigIdPrefix(String prefix) {
        log.debug("Request to get AppConfigs with prefix : {}", prefix);
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AppConfigs> rows = appConfigsRepository.findByConfigIdStartingWith(prefix);
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> out = new LinkedHashMap<>(rows.size());
        for (AppConfigs row : rows) {
            // Plugin/SPI read path: hand back plaintext. decryptIfNeeded is a no-op
            // for non-sensitive (plaintext) rows and for legacy sensitive rows not yet
            // re-encrypted, so this is safe for every row.
            out.put(row.getConfigId(), crypto.decryptIfNeeded(row.getConfigValue()));
        }
        return Collections.unmodifiableMap(out);
    }

    private AppConfigsSPI toSpi(AppConfigs entity) {
        if (entity == null) {
            return null;
        }

        AppConfigsSPI appConfigsSPI = new AppConfigsSPI();
        appConfigsSPI.setId(entity.getId());
        appConfigsSPI.setConfigId(entity.getConfigId());
        // Plugin/SPI read path: decrypt so plugins receive plaintext. No-op for
        // plaintext rows (sentinel-gated), so non-sensitive configs are unaffected.
        appConfigsSPI.setConfigValue(crypto.decryptIfNeeded(entity.getConfigValue()));
        appConfigsSPI.setServiceType(entity.getServiceType());
        appConfigsSPI.setConfigStatus(entity.getConfigStatus());
        appConfigsSPI.setConfigBinValue(entity.getConfigBinValue());

        return appConfigsSPI;
    }
}
