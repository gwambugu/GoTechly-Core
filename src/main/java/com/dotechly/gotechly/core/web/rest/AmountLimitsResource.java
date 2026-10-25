package com.dotechly.gotechly.core.web.rest;

import com.dotechly.gotechly.core.service.impl.AmountLimitsQueryService;
import com.dotechly.gotechly.core.service.criteria.AmountLimitsCriteria;
import com.dotechly.gotechly.core.service.dto.AmountLimitsDTO;
import com.dotechly.gotechly.core.service.impl.AmountLimitsService;
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
public class AmountLimitsResource {

    private final Logger log = LoggerFactory.getLogger(AmountLimitsResource.class);

    private static final String ENTITY_NAME = "amountLimits";

    private final AmountLimitsService amountLimitsService;

    private final AmountLimitsQueryService amountLimitsQueryService;

    public AmountLimitsResource(AmountLimitsService amountLimitsService, AmountLimitsQueryService amountLimitsQueryService) {
        this.amountLimitsService = amountLimitsService;
        this.amountLimitsQueryService = amountLimitsQueryService;
    }

    @PostMapping("/amount-limits")
    public ResponseEntity<AmountLimitsDTO> createAmountLimits(@RequestBody AmountLimitsDTO amountLimitsDTO) throws URISyntaxException {
        log.debug("REST request to save AmountLimits : {}", amountLimitsDTO);
        if (amountLimitsDTO.getId() != null) {
            throw new BadRequestAlertException("A new amountLimits cannot already have an ID", ENTITY_NAME, "idexists");
        }
        AmountLimitsDTO result = amountLimitsService.save(amountLimitsDTO);
        return ResponseEntity.created(new URI("/api/v1/configs/amount-limits/" + result.getId())).body(result);
    }

    @PutMapping("/amount-limits/{id}")
    public ResponseEntity<AmountLimitsDTO> updateAmountLimits(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody AmountLimitsDTO amountLimitsDTO
    ) {
        log.debug("REST request to update AmountLimits : {}, {}", id, amountLimitsDTO);
        if (amountLimitsDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, amountLimitsDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (amountLimitsService.findOne(id).isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        AmountLimitsDTO result = amountLimitsService.save(amountLimitsDTO);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/amount-limits")
    public ResponseEntity<List<AmountLimitsDTO>> getAllAmountLimits(AmountLimitsCriteria criteria, Pageable pageable) {
        log.debug("REST request to get AmountLimits by criteria: {}", criteria);
        Page<AmountLimitsDTO> page = amountLimitsQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/amount-limits/count")
    public ResponseEntity<Long> countAmountLimits(AmountLimitsCriteria criteria) {
        log.debug("REST request to count AmountLimits by criteria: {}", criteria);
        return ResponseEntity.ok().body(amountLimitsQueryService.countByCriteria(criteria));
    }

    @GetMapping("/amount-limits/{id}")
    public ResponseEntity<AmountLimitsDTO> getAmountLimits(@PathVariable Long id) {
        log.debug("REST request to get AmountLimits : {}", id);
        Optional<AmountLimitsDTO> amountLimitsDTO = amountLimitsService.findOne(id);
        return amountLimitsDTO.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/amount-limits/{id}")
    public ResponseEntity<Void> deleteAmountLimits(@PathVariable Long id) {
        log.debug("REST request to delete AmountLimits : {}", id);
        amountLimitsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
