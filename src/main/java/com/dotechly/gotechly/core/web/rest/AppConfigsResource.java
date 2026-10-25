package com.dotechly.gotechly.core.web.rest;

import com.dotechly.gotechly.core.service.impl.AppConfigsQueryService;
import com.dotechly.gotechly.core.service.criteria.AppConfigsCriteria;
import com.dotechly.gotechly.core.service.dto.AppConfigsDTO;
import com.dotechly.gotechly.core.service.impl.AppConfigsService;
import com.dotechly.gotechly.core.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/configs")
public class AppConfigsResource {

    private final Logger log = LoggerFactory.getLogger(AppConfigsResource.class);

    private static final String ENTITY_NAME = "appConfigs";

    private final AppConfigsService appConfigsService;

    private final AppConfigsQueryService appConfigsQueryService;

    public AppConfigsResource(AppConfigsService appConfigsService, AppConfigsQueryService appConfigsQueryService) {
        this.appConfigsService = appConfigsService;
        this.appConfigsQueryService = appConfigsQueryService;
    }

    @PostMapping("/app-configs")
    public ResponseEntity<AppConfigsDTO> createAppConfigs(@RequestBody AppConfigsDTO appConfigsDTO) throws URISyntaxException {
        log.debug("REST request to save AppConfigs : {}", appConfigsDTO);
        if (appConfigsDTO.getId() != null) {
            throw new BadRequestAlertException("A new appConfigs cannot already have an ID", ENTITY_NAME, "idexists");
        }
        validateConfigId(appConfigsDTO, null);
        AppConfigsDTO result = appConfigsService.save(appConfigsDTO);
        return ResponseEntity.created(new URI("/api/v1/configs/app-configs/" + result.getId())).body(result);
    }

    @PutMapping("/app-configs/{id}")
    public ResponseEntity<AppConfigsDTO> updateAppConfigs(
            @PathVariable(value = "id", required = false) final Long id,
            @RequestBody AppConfigsDTO appConfigsDTO
    ) {
        log.debug("REST request to update AppConfigs : {}, {}", id, appConfigsDTO);
        if (appConfigsDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, appConfigsDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (appConfigsService.findOne(id).isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        validateConfigId(appConfigsDTO, id);
        AppConfigsDTO result = appConfigsService.save(appConfigsDTO);
        return ResponseEntity.ok().body(result);
    }

    /**
     * Reject blank config_id and duplicates before they hit the DB unique
     * constraint — a SQL violation surfaces as 500 with an opaque message,
     * whereas a clean 400 with a stable error key lets the UI render a
     * specific message.
     *
     * @param dto      incoming payload
     * @param ownId    when updating, the row's own id so we don't flag
     *                 self-matches as duplicates; null on create
     */
    private void validateConfigId(AppConfigsDTO dto, Long ownId) {
        String configId = dto.getConfigId();
        if (configId == null || configId.trim().isEmpty()) {
            throw new BadRequestAlertException("Config ID is required", ENTITY_NAME, "configIdBlank");
        }
        // Trim in place so downstream callers and the DB store the canonical form.
        dto.setConfigId(configId.trim());

        appConfigsService.findByConfigId(dto.getConfigId())
                .filter(existing -> !Objects.equals(existing.getId(), ownId))
                .ifPresent(existing -> {
                    throw new BadRequestAlertException(
                            "Config ID '" + dto.getConfigId() + "' already exists",
                            ENTITY_NAME, "configIdDuplicate");
                });
    }

    @GetMapping("/app-configs")
    public ResponseEntity<List<AppConfigsDTO>> getAllAppConfigs(AppConfigsCriteria criteria, Pageable pageable) {
        log.debug("REST request to get AppConfigs by criteria: {}", criteria);
        Page<AppConfigsDTO> page = appConfigsQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/app-configs/count")
    public ResponseEntity<Long> countAppConfigs(AppConfigsCriteria criteria) {
        log.debug("REST request to count AppConfigs by criteria: {}", criteria);
        return ResponseEntity.ok().body(appConfigsQueryService.countByCriteria(criteria));
    }

    @GetMapping("/app-configs/{id}")
    public ResponseEntity<AppConfigsDTO> getAppConfigs(@PathVariable Long id) {
        log.debug("REST request to get AppConfigs : {}", id);
        Optional<AppConfigsDTO> appConfigsDTO = appConfigsService.findOne(id);
        return appConfigsDTO.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/app-configs/{id}")
    public ResponseEntity<Void> deleteAppConfigs(@PathVariable Long id) {
        log.debug("REST request to delete AppConfigs : {}", id);
        appConfigsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
