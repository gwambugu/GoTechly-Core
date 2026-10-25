package com.dotechly.gotechly.core.service.impl;


import com.dotechly.gotechly.core.config.cluster.ClusterConfigRefresher;
import com.dotechly.gotechly.core.domain.AuditLog;
import com.dotechly.gotechly.core.domain.PendingChange;
import com.dotechly.gotechly.core.repository.AuditLogRepository;
import com.dotechly.gotechly.core.repository.PendingChangeRepository;
import com.dotechly.gotechly.core.service.dto.*;
import com.dotechly.gotechly.core.service.mapper.PendingChangeMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PendingChangeService {

    private final Logger log = LoggerFactory.getLogger(PendingChangeService.class);

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final PendingChangeRepository repository;
    private final PendingChangeMapper mapper;
    private final ObjectMapper objectMapper;

    private final AppConfigsService appConfigsService;
    private final AppConfigsCryptoService appConfigsCrypto;
    private final ChannelsService channelsService;
    private final EndpointConfigsService endpointConfigsService;
    private final ErrorCodesService errorCodesService;
    private final AmountLimitsService amountLimitsService;
    private final TemplateLookupService templateLookupService;
    private final BankCodesLookupService bankCodesLookupService;
    private final LinkedWalletSubscriptionService linkedWalletSubscriptionService;
    private final AuditLogRepository auditLogRepository;
    private final TransactionTemplate auditTxTemplate;
    private final ClusterConfigRefresher clusterConfigRefresher;

    public PendingChangeService(
            PendingChangeRepository repository,
            PendingChangeMapper mapper,
            AppConfigsService appConfigsService,
            AppConfigsCryptoService appConfigsCrypto,
            ChannelsService channelsService,
            EndpointConfigsService endpointConfigsService,
            ErrorCodesService errorCodesService,
            AmountLimitsService amountLimitsService,
            TemplateLookupService templateLookupService,
            BankCodesLookupService bankCodesLookupService,
            LinkedWalletSubscriptionService linkedWalletSubscriptionService,
            AuditLogRepository auditLogRepository,
            ClusterConfigRefresher clusterConfigRefresher,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.appConfigsService = appConfigsService;
        this.appConfigsCrypto = appConfigsCrypto;
        this.channelsService = channelsService;
        this.endpointConfigsService = endpointConfigsService;
        this.errorCodesService = errorCodesService;
        this.amountLimitsService = amountLimitsService;
        this.templateLookupService = templateLookupService;
        this.bankCodesLookupService = bankCodesLookupService;
        this.linkedWalletSubscriptionService = linkedWalletSubscriptionService;
        this.auditLogRepository = auditLogRepository;
        this.clusterConfigRefresher = clusterConfigRefresher;
        this.auditTxTemplate = new TransactionTemplate(transactionManager);
        this.auditTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    public PendingChangeDTO submit(PendingChangeDTO dto) {
        log.debug("Request to submit PendingChange : {}", dto);

        validateEntityType(dto.getEntityType());
        validateChangeType(dto.getChangeType());

        // Encrypt any sensitive APP_CONFIGS secret in the payload BEFORE it is persisted
        // to pending_changes or copied into the audit log. After this, plaintext secrets
        // never sit at rest anywhere in the maker-checker trail.
        sanitizeSensitiveAppConfigPayload(dto);

        // For UPDATE, capture current state as previousValue
        if ("UPDATE".equals(dto.getChangeType())) {
            if (dto.getEntityId() == null) {
                throw new IllegalArgumentException("entityId is required for UPDATE changes");
            }
            String currentValue = fetchCurrentValue(dto.getEntityType(), dto.getEntityId());
            dto.setPreviousValue(currentValue);
        }

        dto.setStatus(STATUS_PENDING);
        dto.setMakerDate(Instant.now());
        dto.setCheckerUsername(null);
        dto.setCheckerDate(null);
        dto.setCheckerRemarks(null);

        PendingChange entity = mapper.toEntity(dto);
        entity = repository.save(entity);

        writeAuditLog(entity, "CONFIG_CHANGE_SUBMITTED", entity.getMakerUsername(), "MAKER",
                "Change submitted for approval: " + entity.getDescription());

        return mapper.toDto(entity);
    }

    public PendingChangeDTO approve(Long id, String checkerUsername) {
        log.debug("Request to approve PendingChange : {}", id);

        PendingChange entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PendingChange not found: " + id));

        if (!STATUS_PENDING.equals(entity.getStatus())) {
            throw new IllegalStateException("Can only approve PENDING changes, current status: " + entity.getStatus());
        }

        if (entity.getMakerUsername().equals(checkerUsername)) {
            throw new IllegalStateException("Maker and checker cannot be the same user");
        }

        // Apply the change to the target entity
        applyChange(entity);

        // Mark as approved
        entity.setStatus(STATUS_APPROVED);
        entity.setCheckerUsername(checkerUsername);
        entity.setCheckerDate(Instant.now());
        entity = repository.save(entity);

        writeAuditLog(entity,
                "CREATE".equals(entity.getChangeType()) ? "CONFIG_CREATED" : "CONFIG_UPDATED",
                checkerUsername, "CHECKER",
                "Approved by " + checkerUsername + ". Maker: " + entity.getMakerUsername());

        return mapper.toDto(entity);
    }

    public PendingChangeDTO reject(Long id, String checkerUsername, String remarks) {
        log.debug("Request to reject PendingChange : {}", id);

        PendingChange entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PendingChange not found: " + id));

        if (!STATUS_PENDING.equals(entity.getStatus())) {
            throw new IllegalStateException("Can only reject PENDING changes, current status: " + entity.getStatus());
        }

        // The maker may reject their own pending change — this is a self-cancellation, always
        // allowed. The maker-checker separation is enforced on approve() only; a maker withdrawing
        // their own change introduces no four-eyes risk. Either path leaves the row REJECTED.
        boolean selfCancel = entity.getMakerUsername().equals(checkerUsername);

        entity.setStatus(STATUS_REJECTED);
        entity.setCheckerUsername(checkerUsername);
        entity.setCheckerDate(Instant.now());
        entity.setCheckerRemarks(remarks);
        entity = repository.save(entity);

        String rejectNote = (selfCancel ? "Cancelled by maker " : "Rejected by ")
                + checkerUsername + ". Maker: " + entity.getMakerUsername();
        if (remarks != null && !remarks.isBlank()) {
            rejectNote += ". Remarks: " + remarks;
        }
        writeAuditLog(entity,
                selfCancel ? "CONFIG_CHANGE_CANCELLED" : "CONFIG_CHANGE_REJECTED",
                checkerUsername, selfCancel ? "MAKER" : "CHECKER", rejectNote);

        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public Optional<PendingChangeDTO> findOne(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<PendingChangeDTO> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    private void applyChange(PendingChange change) {
        try {
            switch (change.getEntityType()) {
                case "APP_CONFIGS" -> {
                    AppConfigsDTO dto = objectMapper.readValue(change.getPayload(), AppConfigsDTO.class);
                    if ("CREATE".equals(change.getChangeType())) {
                        dto.setId(null);
                    }
                    // A sensitive UPDATE that left the value blank/masked means "keep the
                    // existing secret" — copy the raw stored value forward. (save() then
                    // re-encrypts it if it was a legacy plaintext row; ciphertext passes
                    // through idempotently.)
                    if ("UPDATE".equals(change.getChangeType()) && dto.getId() != null
                            && dto.isSensitive() && appConfigsCrypto.isBlankOrMasked(dto.getConfigValue())) {
                        dto.setConfigValue(appConfigsService.findRawConfigValue(dto.getId()).orElse(null));
                    }
                    // Never persist the mask placeholder as if it were a value (e.g. a
                    // sensitive CREATE submitted with no value).
                    if (dto.isSensitive() && appConfigsCrypto.isBlankOrMasked(dto.getConfigValue())) {
                        dto.setConfigValue(null);
                    }
                    AppConfigsDTO saved = appConfigsService.save(dto);
                    if ("CREATE".equals(change.getChangeType())) {
                        change.setEntityId(saved.getId());
                    }
                    scheduleAppConfigRefresh(saved.getConfigId());
                }
                case "CHANNELS" -> {
                    ChannelsDTO dto = objectMapper.readValue(change.getPayload(), ChannelsDTO.class);
                    if ("CREATE".equals(change.getChangeType())) {
                        dto.setId(null);
                    }
                    ChannelsDTO saved = channelsService.save(dto);
                    if ("CREATE".equals(change.getChangeType())) {
                        change.setEntityId(saved.getId());
                    }
                }
                case "ENDPOINT_CONFIGS" -> {
                    EndpointConfigsDTO dto = objectMapper.readValue(change.getPayload(), EndpointConfigsDTO.class);
                    if ("CREATE".equals(change.getChangeType())) {
                        dto.setId(null);
                    }
                    EndpointConfigsDTO saved = endpointConfigsService.save(dto);
                    if ("CREATE".equals(change.getChangeType())) {
                        change.setEntityId(saved.getId());
                    }
                }
                case "ERROR_CODES" -> {
                    ErrorCodesDTO dto = objectMapper.readValue(change.getPayload(), ErrorCodesDTO.class);
                    if ("CREATE".equals(change.getChangeType())) {
                        dto.setId(null);
                    }
                    ErrorCodesDTO saved = errorCodesService.save(dto);
                    if ("CREATE".equals(change.getChangeType())) {
                        change.setEntityId(saved.getId());
                    }
                    scheduleErrorCodeRefresh(saved.getApiName());
                }
                case "AMOUNT_LIMITS" -> {
                    AmountLimitsDTO dto = objectMapper.readValue(change.getPayload(), AmountLimitsDTO.class);
                    if ("CREATE".equals(change.getChangeType())) {
                        dto.setId(null);
                    }
                    AmountLimitsDTO saved = amountLimitsService.save(dto);
                    if ("CREATE".equals(change.getChangeType())) {
                        change.setEntityId(saved.getId());
                    }
                }
                case "TEMPLATE_LOOKUP" -> {
                    TemplateLookupDTO dto = objectMapper.readValue(change.getPayload(), TemplateLookupDTO.class);
                    if ("CREATE".equals(change.getChangeType())) {
                        dto.setId(null);
                    }
                    // The Gateway maker-checker UI does not edit hm — preserve the existing
                    // value on UPDATE so an approval doesn't blank a server-set field.
                    if ("UPDATE".equals(change.getChangeType()) && dto.getId() != null && dto.getHm() == null) {
                        templateLookupService.findOne(dto.getId())
                                .map(TemplateLookupDTO::getHm)
                                .ifPresent(dto::setHm);
                    }
                    // applyChange runs before checkerUsername is stamped on the entity, so
                    // attribute the modification to the maker. The full maker→checker trail
                    // is already in the AuditLog row written by writeAuditLog.
                    dto.setModifiedBy(change.getMakerUsername());
                    TemplateLookupDTO saved = templateLookupService.save(dto);
                    if ("CREATE".equals(change.getChangeType())) {
                        change.setEntityId(saved.getId());
                    }
                }
                case "BANK_CODES_LOOKUP" -> {
                    BankCodesLookupDTO dto = objectMapper.readValue(change.getPayload(), BankCodesLookupDTO.class);
                    if ("CREATE".equals(change.getChangeType())) {
                        dto.setId(null);
                    }
                    BankCodesLookupDTO saved = bankCodesLookupService.save(dto);
                    if ("CREATE".equals(change.getChangeType())) {
                        change.setEntityId(saved.getId());
                    }
                }
                case "LINKED_WALLET_SUBSCRIPTION" -> {
                    LinkedWalletSubscriptionDTO dto = objectMapper.readValue(change.getPayload(), LinkedWalletSubscriptionDTO.class);
                    if ("CREATE".equals(change.getChangeType())) {
                        dto.setId(null);
                    }
                    LinkedWalletSubscriptionDTO saved = linkedWalletSubscriptionService.save(dto);
                    if ("CREATE".equals(change.getChangeType())) {
                        change.setEntityId(saved.getId());
                    }
                }
                default -> throw new IllegalStateException("Unknown entity type: " + change.getEntityType());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to apply change: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // Live plugin config refresh on approval (no Core restart)
    // =========================================================================
    //
    // Plugins freeze some config at init() time (preloaded error-code maps, HTTP-client pool /
    // timeout / credential settings). The Core-side caches evict on save, but a loaded plugin
    // keeps its own private copy, so a change is otherwise invisible until restart. After an
    // approved APP_CONFIGS / ERROR_CODES change commits, we re-initialize the affected plugin —
    // on THIS instance and, via the Hazelcast cluster, on every other instance too.
    // (ENDPOINT_CONFIGS is already picked up live — handlers read those through the cached lookup
    // per request — so it needs no refresh.)
    //
    // The refresh is registered as an afterCommit callback so plugins re-read COMMITTED DB values
    // and so a refresh failure can never roll back the approval transaction.

    /** app_configs config_id is {paymentType}.{...}; {@code core.*} is a cross-plugin global. */
    private void scheduleAppConfigRefresh(String configId) {
        if (configId == null || configId.isBlank()) {
            return;
        }
        int dot = configId.indexOf('.');
        String head = dot > 0 ? configId.substring(0, dot) : configId;
        if ("core".equalsIgnoreCase(head)) {
            registerAfterCommitRefresh(ClusterConfigRefresher.ALL,
                    "all plugins (core.* global '" + configId + "')");
        } else {
            registerAfterCommitRefresh(head, "app_configs '" + configId + "'");
        }
    }

    /** error_codes api_name is {paymentType}.{serviceScope}; refresh that one payment type. */
    private void scheduleErrorCodeRefresh(String apiName) {
        if (apiName == null || apiName.isBlank()) {
            return;
        }
        int dot = apiName.indexOf('.');
        String pt = dot > 0 ? apiName.substring(0, dot) : apiName;
        registerAfterCommitRefresh(pt, "error_codes api '" + apiName + "'");
    }

    /**
     * After the current transaction commits (so plugins re-read committed config), trigger a
     * cluster-wide refresh of {@code scope} (a payment type, or {@link ClusterConfigRefresher#ALL}).
     * Falls back to running inline if no transaction is active. Refresh failures are logged and
     * swallowed — they must not affect the (already-committed) approval.
     */
    private void registerAfterCommitRefresh(String scope, String describe) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runRefresh(scope, describe);
                }
            });
        } else {
            runRefresh(scope, describe);
        }
    }

    private void runRefresh(String scope, String describe) {
        try {
            log.info("Config approved — refreshing cluster for {} to pick up new values without restart.", describe);
            clusterConfigRefresher.refreshClusterWide(scope);
        } catch (Exception e) {
            log.error("Cluster config refresh for {} failed: {}", describe, e.getMessage(), e);
        }
    }

    /**
     * For an APP_CONFIGS change carrying a sensitive value, rewrite the payload so the
     * stored form never contains plaintext:
     * <ul>
     *   <li>blank / masked value → the canonical mask placeholder, meaning "unchanged"
     *       (the diff shows no change and {@link #applyChange} preserves the existing secret);</li>
     *   <li>a real new value → AES-GCM ciphertext ({@code enc:v1:...}).</li>
     * </ul>
     * No-op for non-sensitive configs and all other entity types.
     */
    private void sanitizeSensitiveAppConfigPayload(PendingChangeDTO dto) {
        if (!"APP_CONFIGS".equals(dto.getEntityType()) || dto.getPayload() == null) {
            return;
        }
        try {
            AppConfigsDTO cfg = objectMapper.readValue(dto.getPayload(), AppConfigsDTO.class);
            if (!cfg.isSensitive()) {
                return;
            }
            cfg.setConfigValue(appConfigsCrypto.isBlankOrMasked(cfg.getConfigValue())
                    ? AppConfigsCryptoService.MASKED_VALUE
                    : appConfigsCrypto.encryptIfNeeded(cfg.getConfigValue()));
            dto.setPayload(objectMapper.writeValueAsString(cfg));
        } catch (Exception e) {
            // Fail closed: a payload we can't sanitize must not be stored as-is.
            throw new IllegalArgumentException("Invalid APP_CONFIGS payload: " + e.getMessage(), e);
        }
    }

    private String fetchCurrentValue(String entityType, Long entityId) {
        try {
            Object current = switch (entityType) {
                case "APP_CONFIGS" -> appConfigsService.findOne(entityId).orElse(null);
                case "CHANNELS" -> channelsService.findOne(entityId).orElse(null);
                case "ENDPOINT_CONFIGS" -> endpointConfigsService.findOne(entityId).orElse(null);
                case "ERROR_CODES" -> errorCodesService.findOne(entityId).orElse(null);
                case "AMOUNT_LIMITS" -> amountLimitsService.findOne(entityId).orElse(null);
                case "TEMPLATE_LOOKUP" -> templateLookupService.findOne(entityId).orElse(null);
                case "BANK_CODES_LOOKUP" -> bankCodesLookupService.findOne(entityId).orElse(null);
                case "LINKED_WALLET_SUBSCRIPTION" -> linkedWalletSubscriptionService.findOne(entityId).orElse(null);
                default -> throw new IllegalStateException("Unknown entity type: " + entityType);
            };
            return current != null ? objectMapper.writeValueAsString(current) : null;
        } catch (Exception e) {
            log.warn("Failed to fetch current value for {} id={}: {}", entityType, entityId, e.getMessage());
            return null;
        }
    }

    private void writeAuditLog(PendingChange change, String eventType, String actor, String role, String description) {
        // Run in a separate transaction so audit failures never roll back the main transaction
        try {
            auditTxTemplate.executeWithoutResult(status -> {
                AuditLog audit = new AuditLog();
                audit.setEventType(truncate(eventType, 100));
                audit.setEntityName(truncate(change.getEntityType(), 100));
                audit.setOldValue(change.getPreviousValue());
                audit.setNewValue(change.getPayload());
                audit.setChangedFields(computeChangedFields(change.getPreviousValue(), change.getPayload()));
                audit.setActorId(truncate(actor, 100));
                audit.setActorType("USER");
                audit.setActorUsername(truncate(change.getMakerUsername(), 100));
                audit.setRequestId(truncate(String.valueOf(change.getId()), 200));
                audit.setEndpoint(truncate("pending-change/" + change.getId(), 300));
                audit.setServiceName("GoTechly-Core");
                audit.setPhaseName(truncate(change.getChangeType().toLowerCase() + ":" + change.getStatus().toLowerCase(), 100));
                audit.setRole(truncate(role, 100));
                audit.setSessionId(truncate(change.getEntityType() + ":" + change.getEntityId(), 100));
                audit.setEnvironment(truncate(description, 20));
                audit.setCreatedAt(Instant.now());
                auditLogRepository.save(audit);
            });
        } catch (Exception e) {
            log.error("Failed to write audit log for PendingChange {}: {}", change.getId(), e.getMessage());
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    private String computeChangedFields(String previousJson, String newJson) {
        try {
            Map<String, Object> previousMap = previousJson != null
                    ? objectMapper.readValue(previousJson, new TypeReference<>() {})
                    : Collections.emptyMap();
            Map<String, Object> newMap = newJson != null
                    ? objectMapper.readValue(newJson, new TypeReference<>() {})
                    : Collections.emptyMap();

            Set<String> allKeys = new TreeSet<>();
            allKeys.addAll(previousMap.keySet());
            allKeys.addAll(newMap.keySet());

            List<String> changed = allKeys.stream()
                    .filter(k -> !Objects.equals(previousMap.get(k), newMap.get(k)))
                    .filter(k -> !"id".equals(k))
                    .collect(Collectors.toList());

            return changed.isEmpty() ? null : objectMapper.writeValueAsString(changed);
        } catch (Exception e) {
            log.debug("Failed to compute changed fields: {}", e.getMessage());
            return null;
        }
    }

    private void validateEntityType(String entityType) {
        if (entityType == null || !entityType.matches("APP_CONFIGS|CHANNELS|ENDPOINT_CONFIGS|ERROR_CODES|AMOUNT_LIMITS|TEMPLATE_LOOKUP|BANK_CODES_LOOKUP|LINKED_WALLET_SUBSCRIPTION")) {
            throw new IllegalArgumentException(
                    "Invalid entityType: '" + entityType + "'. Must be one of: APP_CONFIGS, CHANNELS, ENDPOINT_CONFIGS, ERROR_CODES, AMOUNT_LIMITS, TEMPLATE_LOOKUP, BANK_CODES_LOOKUP, LINKED_WALLET_SUBSCRIPTION");
        }
    }

    private void validateChangeType(String changeType) {
        if (changeType == null || !changeType.matches("CREATE|UPDATE")) {
            throw new IllegalArgumentException("Invalid changeType: '" + changeType + "'. Must be CREATE or UPDATE");
        }
    }
}
