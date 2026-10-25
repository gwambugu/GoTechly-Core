package com.dotechly.gotechly.core.web.rest;

import com.dotechly.gotechly.core.service.impl.AuditLogQueryService;
import com.dotechly.gotechly.core.service.criteria.AuditLogCriteria;
import com.dotechly.gotechly.core.service.dto.AuditLogDTO;
import com.dotechly.gotechly.core.service.impl.AuditLogService;
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

/**
 * REST controller for managing {@link com.dotechly.gotechly.core.domain.AuditLog}.
 */
@RestController
@RequestMapping("/api")
public class AuditLogResource {

    private final Logger log = LoggerFactory.getLogger(AuditLogResource.class);

    private static final String ENTITY_NAME = "auditLog";

    private final AuditLogService auditLogService;

    private final AuditLogQueryService auditLogQueryService;

    public AuditLogResource(AuditLogService auditLogService, AuditLogQueryService auditLogQueryService) {
        this.auditLogService = auditLogService;
        this.auditLogQueryService = auditLogQueryService;
    }

    /**
     * {@code POST  /audit-logs} : Create a new auditLog.
     *
     * @param auditLogDTO the auditLogDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new auditLogDTO, or with status {@code 400 (Bad Request)} if the auditLog has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/audit-logs")
    public ResponseEntity<AuditLogDTO> createAuditLog(@RequestBody AuditLogDTO auditLogDTO) throws URISyntaxException {
        log.debug("REST request to save AuditLog : {}", auditLogDTO);
        if (auditLogDTO.getId() != null) {
            throw new BadRequestAlertException("A new auditLog cannot already have an ID", ENTITY_NAME, "idexists");
        }
        AuditLogDTO result = auditLogService.save(auditLogDTO);
        return ResponseEntity.created(new URI("/api/audit-logs/" + result.getId())).body(result);
    }

    /**
     * {@code PUT  /audit-logs/:id} : Updates an existing auditLog.
     *
     * @param id the id of the auditLogDTO to save.
     * @param auditLogDTO the auditLogDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated auditLogDTO,
     * or with status {@code 400 (Bad Request)} if the auditLogDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the auditLogDTO couldn't be updated.
     */
    @PutMapping("/audit-logs/{id}")
    public ResponseEntity<AuditLogDTO> updateAuditLog(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody AuditLogDTO auditLogDTO
    ) {
        log.debug("REST request to update AuditLog : {}, {}", id, auditLogDTO);
        if (auditLogDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, auditLogDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (auditLogService.findOne(id).isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        AuditLogDTO result = auditLogService.save(auditLogDTO);
        return ResponseEntity.ok().body(result);
    }

    /**
     * {@code GET  /audit-logs} : get all the auditLogs.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of auditLogs in body.
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogDTO>> getAllAuditLogs(AuditLogCriteria criteria, Pageable pageable) {
        log.debug("REST request to get AuditLogs by criteria: {}", criteria);
        Page<AuditLogDTO> page = auditLogQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /audit-logs/count} : count all the auditLogs.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/audit-logs/count")
    public ResponseEntity<Long> countAuditLogs(AuditLogCriteria criteria) {
        log.debug("REST request to count AuditLogs by criteria: {}", criteria);
        return ResponseEntity.ok().body(auditLogQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /audit-logs/:id} : get the "id" auditLog.
     *
     * @param id the id of the auditLogDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the auditLogDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/audit-logs/{id}")
    public ResponseEntity<AuditLogDTO> getAuditLog(@PathVariable Long id) {
        log.debug("REST request to get AuditLog : {}", id);
        Optional<AuditLogDTO> auditLogDTO = auditLogService.findOne(id);
        return auditLogDTO.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * {@code DELETE  /audit-logs/:id} : delete the "id" auditLog.
     *
     * @param id the id of the auditLogDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/audit-logs/{id}")
    public ResponseEntity<Void> deleteAuditLog(@PathVariable Long id) {
        log.debug("REST request to delete AuditLog : {}", id);
        auditLogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
