package com.dotechly.gotechly.core.web.rest;

import com.dotechly.gotechly.core.service.impl.PaymentLogQueryService;
import com.dotechly.gotechly.core.service.criteria.PaymentLogCriteria;
import com.dotechly.gotechly.core.service.dto.PaymentLogDTO;
import com.dotechly.gotechly.core.service.impl.PaymentLogService;
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
public class PaymentLogResource {

    private final Logger log = LoggerFactory.getLogger(PaymentLogResource.class);

    private static final String ENTITY_NAME = "paymentLog";

    private final PaymentLogService paymentLogService;

    private final PaymentLogQueryService paymentLogQueryService;

    public PaymentLogResource(PaymentLogService paymentLogService, PaymentLogQueryService paymentLogQueryService) {
        this.paymentLogService = paymentLogService;
        this.paymentLogQueryService = paymentLogQueryService;
    }

    @PostMapping("/payment-logs")
    public ResponseEntity<PaymentLogDTO> createPaymentLog(@RequestBody PaymentLogDTO paymentLogDTO) throws URISyntaxException {
        log.debug("REST request to save PaymentLog : {}", paymentLogDTO);
        if (paymentLogDTO.getId() != null) {
            throw new BadRequestAlertException("A new paymentLog cannot already have an ID", ENTITY_NAME, "idexists");
        }
        PaymentLogDTO result = paymentLogService.save(paymentLogDTO);
        return ResponseEntity.created(new URI("/api/payment-logs/" + result.getId())).body(result);
    }

    @PutMapping("/payment-logs/{id}")
    public ResponseEntity<PaymentLogDTO> updatePaymentLog(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody PaymentLogDTO paymentLogDTO
    ) {
        log.debug("REST request to update PaymentLog : {}, {}", id, paymentLogDTO);
        if (paymentLogDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, paymentLogDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (paymentLogService.findOne(id).isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        PaymentLogDTO result = paymentLogService.save(paymentLogDTO);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/payment-logs")
    public ResponseEntity<List<PaymentLogDTO>> getAllPaymentLogs(PaymentLogCriteria criteria, Pageable pageable) {
        log.debug("REST request to get PaymentLogs by criteria: {}", criteria);
        Page<PaymentLogDTO> page = paymentLogQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/payment-logs/count")
    public ResponseEntity<Long> countPaymentLogs(PaymentLogCriteria criteria) {
        log.debug("REST request to count PaymentLogs by criteria: {}", criteria);
        return ResponseEntity.ok().body(paymentLogQueryService.countByCriteria(criteria));
    }

    @GetMapping("/payment-logs/{id}")
    public ResponseEntity<PaymentLogDTO> getPaymentLog(@PathVariable Long id) {
        log.debug("REST request to get PaymentLog : {}", id);
        Optional<PaymentLogDTO> paymentLogDTO = paymentLogService.findOne(id);
        return paymentLogDTO.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/payment-logs/{id}")
    public ResponseEntity<Void> deletePaymentLog(@PathVariable Long id) {
        log.debug("REST request to delete PaymentLog : {}", id);
        paymentLogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
