package com.dotechly.gotechly.core.web.rest;

import com.dotechly.gotechly.core.service.impl.ErrorCodesQueryService;
import com.dotechly.gotechly.core.service.criteria.ErrorCodesCriteria;
import com.dotechly.gotechly.core.service.dto.ErrorCodesDTO;
import com.dotechly.gotechly.core.service.impl.ErrorCodesService;
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
public class ErrorCodesResource {

    private final Logger log = LoggerFactory.getLogger(ErrorCodesResource.class);

    private static final String ENTITY_NAME = "errorCodes";

    private final ErrorCodesService errorCodesService;

    private final ErrorCodesQueryService errorCodesQueryService;

    public ErrorCodesResource(ErrorCodesService errorCodesService, ErrorCodesQueryService errorCodesQueryService) {
        this.errorCodesService = errorCodesService;
        this.errorCodesQueryService = errorCodesQueryService;
    }

    @PostMapping("/error-codes")
    public ResponseEntity<ErrorCodesDTO> createErrorCodes(@RequestBody ErrorCodesDTO errorCodesDTO) throws URISyntaxException {
        log.debug("REST request to save ErrorCodes : {}", errorCodesDTO);
        if (errorCodesDTO.getId() != null) {
            throw new BadRequestAlertException("A new errorCodes cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ErrorCodesDTO result = errorCodesService.save(errorCodesDTO);
        return ResponseEntity.created(new URI("/api/v1/configs/error-codes/" + result.getId())).body(result);
    }

    @PutMapping("/error-codes/{id}")
    public ResponseEntity<ErrorCodesDTO> updateErrorCodes(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody ErrorCodesDTO errorCodesDTO
    ) {
        log.debug("REST request to update ErrorCodes : {}, {}", id, errorCodesDTO);
        if (errorCodesDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, errorCodesDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (errorCodesService.findOne(id).isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        ErrorCodesDTO result = errorCodesService.save(errorCodesDTO);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/error-codes")
    public ResponseEntity<List<ErrorCodesDTO>> getAllErrorCodes(ErrorCodesCriteria criteria, Pageable pageable) {
        log.debug("REST request to get ErrorCodes by criteria: {}", criteria);
        Page<ErrorCodesDTO> page = errorCodesQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/error-codes/count")
    public ResponseEntity<Long> countErrorCodes(ErrorCodesCriteria criteria) {
        log.debug("REST request to count ErrorCodes by criteria: {}", criteria);
        return ResponseEntity.ok().body(errorCodesQueryService.countByCriteria(criteria));
    }

    @GetMapping("/error-codes/{id}")
    public ResponseEntity<ErrorCodesDTO> getErrorCodes(@PathVariable Long id) {
        log.debug("REST request to get ErrorCodes : {}", id);
        Optional<ErrorCodesDTO> errorCodesDTO = errorCodesService.findOne(id);
        return errorCodesDTO.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/error-codes/{id}")
    public ResponseEntity<Void> deleteErrorCodes(@PathVariable Long id) {
        log.debug("REST request to delete ErrorCodes : {}", id);
        errorCodesService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
