package com.dotechly.gotechly.core.web.rest;

import com.dotechly.gotechly.core.service.impl.PendingPaymentLogQueryService;
import com.dotechly.gotechly.core.service.criteria.PendingPaymentLogCriteria;
import com.dotechly.gotechly.core.service.dto.PendingPaymentLogDTO;
import com.dotechly.gotechly.core.service.impl.PendingPaymentLogService;
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
@RequestMapping("/api")
public class PendingPaymentLogResource {

    private final Logger log = LoggerFactory.getLogger(PendingPaymentLogResource.class);
    private static final String ENTITY_NAME = "pendingPaymentLog";
    private final PendingPaymentLogService pendingPaymentLogService;
    private final PendingPaymentLogQueryService pendingPaymentLogQueryService;

    public PendingPaymentLogResource(PendingPaymentLogService pendingPaymentLogService, PendingPaymentLogQueryService pendingPaymentLogQueryService) {
        this.pendingPaymentLogService = pendingPaymentLogService;
        this.pendingPaymentLogQueryService = pendingPaymentLogQueryService;
    }

    @PostMapping("/pending-payment-logs")
    public ResponseEntity<PendingPaymentLogDTO> createPendingPaymentLog(@RequestBody PendingPaymentLogDTO dto) throws URISyntaxException {
        log.debug("REST request to save PendingPaymentLog : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new pendingPaymentLog cannot already have an ID", ENTITY_NAME, "idexists");
        }
        PendingPaymentLogDTO result = pendingPaymentLogService.save(dto);
        return ResponseEntity.created(new URI("/api/pending-payment-logs/" + result.getId())).body(result);
    }

    @GetMapping("/pending-payment-logs")
    public ResponseEntity<List<PendingPaymentLogDTO>> getAllPendingPaymentLogs(PendingPaymentLogCriteria criteria, Pageable pageable) {
        log.debug("REST request to get PendingPaymentLogs by criteria: {}", criteria);
        Page<PendingPaymentLogDTO> page = pendingPaymentLogQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/pending-payment-logs/{id}")
    public ResponseEntity<PendingPaymentLogDTO> getPendingPaymentLog(@PathVariable Long id) {
        log.debug("REST request to get PendingPaymentLog : {}", id);
        Optional<PendingPaymentLogDTO> dto = pendingPaymentLogService.findOne(id);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/pending-payment-logs/{id}")
    public ResponseEntity<Void> deletePendingPaymentLog(@PathVariable Long id) {
        log.debug("REST request to delete PendingPaymentLog : {}", id);
        pendingPaymentLogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
