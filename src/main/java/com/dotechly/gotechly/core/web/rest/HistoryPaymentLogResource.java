package com.dotechly.gotechly.core.web.rest;

import com.dotechly.gotechly.core.service.impl.HistoryPaymentLogQueryService;
import com.dotechly.gotechly.core.service.criteria.HistoryPaymentLogCriteria;
import com.dotechly.gotechly.core.service.dto.HistoryPaymentLogDTO;
import com.dotechly.gotechly.core.service.impl.HistoryPaymentLogService;
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
public class HistoryPaymentLogResource {

    private final Logger log = LoggerFactory.getLogger(HistoryPaymentLogResource.class);
    private static final String ENTITY_NAME = "historyPaymentLog";
    private final HistoryPaymentLogService historyPaymentLogService;
    private final HistoryPaymentLogQueryService historyPaymentLogQueryService;

    public HistoryPaymentLogResource(HistoryPaymentLogService historyPaymentLogService, HistoryPaymentLogQueryService historyPaymentLogQueryService) {
        this.historyPaymentLogService = historyPaymentLogService;
        this.historyPaymentLogQueryService = historyPaymentLogQueryService;
    }

    @PostMapping("/history-payment-logs")
    public ResponseEntity<HistoryPaymentLogDTO> createHistoryPaymentLog(@RequestBody HistoryPaymentLogDTO dto) throws URISyntaxException {
        log.debug("REST request to save HistoryPaymentLog : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new historyPaymentLog cannot already have an ID", ENTITY_NAME, "idexists");
        }
        HistoryPaymentLogDTO result = historyPaymentLogService.save(dto);
        return ResponseEntity.created(new URI("/api/history-payment-logs/" + result.getId())).body(result);
    }

    @GetMapping("/history-payment-logs")
    public ResponseEntity<List<HistoryPaymentLogDTO>> getAllHistoryPaymentLogs(HistoryPaymentLogCriteria criteria, Pageable pageable) {
        log.debug("REST request to get HistoryPaymentLogs by criteria: {}", criteria);
        Page<HistoryPaymentLogDTO> page = historyPaymentLogQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/history-payment-logs/{id}")
    public ResponseEntity<HistoryPaymentLogDTO> getHistoryPaymentLog(@PathVariable Long id) {
        log.debug("REST request to get HistoryPaymentLog : {}", id);
        Optional<HistoryPaymentLogDTO> dto = historyPaymentLogService.findOne(id);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/history-payment-logs/{id}")
    public ResponseEntity<Void> deleteHistoryPaymentLog(@PathVariable Long id) {
        log.debug("REST request to delete HistoryPaymentLog : {}", id);
        historyPaymentLogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
