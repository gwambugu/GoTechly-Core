package com.dotechly.gotechly.core.web.rest;


import com.dotechly.gotechly.core.service.criteria.BankCodesLookupCriteria;
import com.dotechly.gotechly.core.service.dto.BankCodesLookupDTO;
import com.dotechly.gotechly.core.service.impl.BankCodesLookupQueryService;
import com.dotechly.gotechly.core.service.impl.BankCodesLookupService;
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
public class BankCodesLookupResource {

    private final Logger log = LoggerFactory.getLogger(BankCodesLookupResource.class);

    private static final String ENTITY_NAME = "bankCodesLookup";

    private final BankCodesLookupService service;
    private final BankCodesLookupQueryService queryService;

    public BankCodesLookupResource(BankCodesLookupService service, BankCodesLookupQueryService queryService) {
        this.service = service;
        this.queryService = queryService;
    }

    @PostMapping("/bank-codes-lookups")
    public ResponseEntity<BankCodesLookupDTO> createBankCodesLookup(@RequestBody BankCodesLookupDTO dto) throws URISyntaxException {
        log.debug("REST request to save BankCodesLookup : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new bankCodesLookup cannot already have an ID", ENTITY_NAME, "idexists");
        }
        BankCodesLookupDTO result = service.save(dto);
        return ResponseEntity.created(new URI("/api/v1/configs/bank-codes-lookups/" + result.getId())).body(result);
    }

    @PutMapping("/bank-codes-lookups/{id}")
    public ResponseEntity<BankCodesLookupDTO> updateBankCodesLookup(
            @PathVariable(value = "id", required = false) final Long id,
            @RequestBody BankCodesLookupDTO dto) {
        log.debug("REST request to update BankCodesLookup : {}, {}", id, dto);
        if (dto.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, dto.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (service.findOne(id).isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        BankCodesLookupDTO result = service.save(dto);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/bank-codes-lookups")
    public ResponseEntity<List<BankCodesLookupDTO>> getAllBankCodesLookups(BankCodesLookupCriteria criteria, Pageable pageable) {
        log.debug("REST request to get BankCodesLookups by criteria: {}", criteria);
        Page<BankCodesLookupDTO> page = queryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/bank-codes-lookups/count")
    public ResponseEntity<Long> countBankCodesLookups(BankCodesLookupCriteria criteria) {
        log.debug("REST request to count BankCodesLookups by criteria: {}", criteria);
        return ResponseEntity.ok().body(queryService.countByCriteria(criteria));
    }

    @GetMapping("/bank-codes-lookups/{id}")
    public ResponseEntity<BankCodesLookupDTO> getBankCodesLookup(@PathVariable Long id) {
        log.debug("REST request to get BankCodesLookup : {}", id);
        Optional<BankCodesLookupDTO> dto = service.findOne(id);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/bank-codes-lookups/{id}")
    public ResponseEntity<Void> deleteBankCodesLookup(@PathVariable Long id) {
        log.debug("REST request to delete BankCodesLookup : {}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
