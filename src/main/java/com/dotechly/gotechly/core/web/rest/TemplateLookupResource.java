package com.dotechly.gotechly.core.web.rest;


import com.dotechly.gotechly.core.service.criteria.TemplateLookupCriteria;
import com.dotechly.gotechly.core.service.dto.TemplateLookupDTO;
import com.dotechly.gotechly.core.service.impl.TemplateLookupQueryService;
import com.dotechly.gotechly.core.service.impl.TemplateLookupService;
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
public class TemplateLookupResource {

    private final Logger log = LoggerFactory.getLogger(TemplateLookupResource.class);

    private static final String ENTITY_NAME = "templateLookup";

    private final TemplateLookupService service;
    private final TemplateLookupQueryService queryService;

    public TemplateLookupResource(TemplateLookupService service, TemplateLookupQueryService queryService) {
        this.service = service;
        this.queryService = queryService;
    }

    @PostMapping("/template-lookups")
    public ResponseEntity<TemplateLookupDTO> createTemplateLookup(@RequestBody TemplateLookupDTO dto) throws URISyntaxException {
        log.debug("REST request to save TemplateLookup : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new templateLookup cannot already have an ID", ENTITY_NAME, "idexists");
        }
        validateTemplateName(dto, null);
        TemplateLookupDTO result = service.save(dto);
        return ResponseEntity.created(new URI("/api/v1/configs/template-lookups/" + result.getId())).body(result);
    }

    @PutMapping("/template-lookups/{id}")
    public ResponseEntity<TemplateLookupDTO> updateTemplateLookup(
            @PathVariable(value = "id", required = false) final Long id,
            @RequestBody TemplateLookupDTO dto) {
        log.debug("REST request to update TemplateLookup : {}, {}", id, dto);
        if (dto.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, dto.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (service.findOne(id).isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        validateTemplateName(dto, id);
        TemplateLookupDTO result = service.save(dto);
        return ResponseEntity.ok().body(result);
    }

    /**
     * Reject blank template_name and duplicates before they hit the DB unique
     * constraint — a SQL violation surfaces as 500 with an opaque message,
     * whereas a clean 400 with a stable errorKey lets the UI render a specific
     * message and lets the maker-checker flow surface the conflict on submit.
     */
    private void validateTemplateName(TemplateLookupDTO dto, Long ownId) {
        String name = dto.getTemplateName();
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestAlertException("Template name is required", ENTITY_NAME, "templateNameBlank");
        }
        dto.setTemplateName(name.trim());
        service.findByTemplateName(dto.getTemplateName())
                .filter(existing -> !Objects.equals(existing.getId(), ownId))
                .ifPresent(existing -> {
                    throw new BadRequestAlertException(
                            "Template name '" + dto.getTemplateName() + "' already exists",
                            ENTITY_NAME, "templateNameDuplicate");
                });
    }

    @GetMapping("/template-lookups")
    public ResponseEntity<List<TemplateLookupDTO>> getAllTemplateLookups(TemplateLookupCriteria criteria, Pageable pageable) {
        log.debug("REST request to get TemplateLookups by criteria: {}", criteria);
        Page<TemplateLookupDTO> page = queryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/template-lookups/count")
    public ResponseEntity<Long> countTemplateLookups(TemplateLookupCriteria criteria) {
        log.debug("REST request to count TemplateLookups by criteria: {}", criteria);
        return ResponseEntity.ok().body(queryService.countByCriteria(criteria));
    }

    @GetMapping("/template-lookups/{id}")
    public ResponseEntity<TemplateLookupDTO> getTemplateLookup(@PathVariable Long id) {
        log.debug("REST request to get TemplateLookup : {}", id);
        Optional<TemplateLookupDTO> dto = service.findOne(id);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/template-lookups/{id}")
    public ResponseEntity<Void> deleteTemplateLookup(@PathVariable Long id) {
        log.debug("REST request to delete TemplateLookup : {}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
