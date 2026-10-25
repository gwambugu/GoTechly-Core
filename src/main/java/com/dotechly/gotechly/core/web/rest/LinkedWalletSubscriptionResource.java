package com.dotechly.gotechly.core.web.rest;


import com.dotechly.gotechly.core.service.criteria.LinkedWalletSubscriptionCriteria;
import com.dotechly.gotechly.core.service.dto.LinkedWalletSubscriptionDTO;
import com.dotechly.gotechly.core.service.impl.LinkedWalletSubscriptionQueryService;
import com.dotechly.gotechly.core.service.impl.LinkedWalletSubscriptionService;
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
 * Read/write REST surface for linked-wallet subscriptions. Reads are used by the admin SPA; writes
 * (create/update) go through the maker-checker approval path ({@code PendingChangeService},
 * entityType {@code LINKED_WALLET_SUBSCRIPTION}) — the SPA ships create/edit buttons disabled for
 * now. The direct POST/PUT here are the endpoints {@code applyChange()} invokes on approval.
 */
@RestController
@RequestMapping("/api/v1/configs")
public class LinkedWalletSubscriptionResource {

    private final Logger log = LoggerFactory.getLogger(LinkedWalletSubscriptionResource.class);

    private static final String ENTITY_NAME = "linkedWalletSubscription";

    private final LinkedWalletSubscriptionService service;
    private final LinkedWalletSubscriptionQueryService queryService;

    public LinkedWalletSubscriptionResource(LinkedWalletSubscriptionService service,
                                            LinkedWalletSubscriptionQueryService queryService) {
        this.service = service;
        this.queryService = queryService;
    }

    @PostMapping("/linked-wallet-subscriptions")
    public ResponseEntity<LinkedWalletSubscriptionDTO> createLinkedWalletSubscription(
            @RequestBody LinkedWalletSubscriptionDTO dto) throws URISyntaxException {
        log.debug("REST request to save LinkedWalletSubscription : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new linkedWalletSubscription cannot already have an ID", ENTITY_NAME, "idexists");
        }
        LinkedWalletSubscriptionDTO result = service.save(dto);
        return ResponseEntity.created(new URI("/api/v1/configs/linked-wallet-subscriptions/" + result.getId())).body(result);
    }

    @PutMapping("/linked-wallet-subscriptions/{id}")
    public ResponseEntity<LinkedWalletSubscriptionDTO> updateLinkedWalletSubscription(
            @PathVariable(value = "id", required = false) final Long id,
            @RequestBody LinkedWalletSubscriptionDTO dto) {
        log.debug("REST request to update LinkedWalletSubscription : {}, {}", id, dto);
        if (dto.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, dto.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (service.findOne(id).isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        LinkedWalletSubscriptionDTO result = service.save(dto);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/linked-wallet-subscriptions")
    public ResponseEntity<List<LinkedWalletSubscriptionDTO>> getAllLinkedWalletSubscriptions(
            LinkedWalletSubscriptionCriteria criteria, Pageable pageable) {
        log.debug("REST request to get LinkedWalletSubscriptions by criteria: {}", criteria);
        Page<LinkedWalletSubscriptionDTO> page = queryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/linked-wallet-subscriptions/count")
    public ResponseEntity<Long> countLinkedWalletSubscriptions(LinkedWalletSubscriptionCriteria criteria) {
        log.debug("REST request to count LinkedWalletSubscriptions by criteria: {}", criteria);
        return ResponseEntity.ok().body(queryService.countByCriteria(criteria));
    }

    @GetMapping("/linked-wallet-subscriptions/{id}")
    public ResponseEntity<LinkedWalletSubscriptionDTO> getLinkedWalletSubscription(@PathVariable Long id) {
        log.debug("REST request to get LinkedWalletSubscription : {}", id);
        Optional<LinkedWalletSubscriptionDTO> dto = service.findOne(id);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/linked-wallet-subscriptions/{id}")
    public ResponseEntity<Void> deleteLinkedWalletSubscription(@PathVariable Long id) {
        log.debug("REST request to delete LinkedWalletSubscription : {}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
