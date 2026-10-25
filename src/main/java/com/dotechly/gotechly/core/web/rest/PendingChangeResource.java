package com.dotechly.gotechly.core.web.rest;

import com.dotechly.gotechly.core.service.criteria.PendingChangeCriteria;
import com.dotechly.gotechly.core.service.dto.PendingChangeDTO;
import com.dotechly.gotechly.core.service.impl.PendingChangeQueryService;
import com.dotechly.gotechly.core.service.impl.PendingChangeService;
import com.dotechly.gotechly.core.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/configs")
public class PendingChangeResource {

    private final Logger log = LoggerFactory.getLogger(PendingChangeResource.class);

    private static final String ENTITY_NAME = "pendingChange";

    private final PendingChangeService pendingChangeService;
    private final PendingChangeQueryService pendingChangeQueryService;

    public PendingChangeResource(PendingChangeService pendingChangeService,
                                 PendingChangeQueryService pendingChangeQueryService) {
        this.pendingChangeService = pendingChangeService;
        this.pendingChangeQueryService = pendingChangeQueryService;
    }

    @PostMapping("/pending-changes")
    public ResponseEntity<PendingChangeDTO> submitChange(
            @RequestBody PendingChangeDTO dto, HttpServletRequest request) throws URISyntaxException {
        log.debug("REST request to submit PendingChange : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new pending change cannot already have an ID", ENTITY_NAME, "idexists");
        }
        dto.setMakerUsername(resolveUsername(request));
        PendingChangeDTO result = pendingChangeService.submit(dto);
        return ResponseEntity.created(new URI("/api/v1/configs/pending-changes/" + result.getId())).body(result);
    }

    @GetMapping("/pending-changes")
    public ResponseEntity<List<PendingChangeDTO>> getAllPendingChanges(
            PendingChangeCriteria criteria, Pageable pageable) {
        log.debug("REST request to get PendingChanges by criteria: {}", criteria);
        Page<PendingChangeDTO> page = pendingChangeQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/pending-changes/{id}")
    public ResponseEntity<PendingChangeDTO> getPendingChange(@PathVariable Long id) {
        log.debug("REST request to get PendingChange : {}", id);
        Optional<PendingChangeDTO> dto = pendingChangeService.findOne(id);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/pending-changes/{id}/approve")
    public ResponseEntity<PendingChangeDTO> approveChange(
            @PathVariable Long id, HttpServletRequest request) {
        log.debug("REST request to approve PendingChange : {}", id);
        PendingChangeDTO result = pendingChangeService.approve(id, resolveUsername(request));
        return ResponseEntity.ok().body(result);
    }

    @PutMapping("/pending-changes/{id}/reject")
    public ResponseEntity<PendingChangeDTO> rejectChange(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        log.debug("REST request to reject PendingChange : {}", id);
        String remarks = body != null ? body.get("remarks") : null;
        PendingChangeDTO result = pendingChangeService.reject(id, resolveUsername(request), remarks);
        return ResponseEntity.ok().body(result);
    }

    private String resolveUsername(HttpServletRequest request) {
        String authUser = request.getHeader("X-Auth-User");
        return (authUser != null && !authUser.isBlank()) ? authUser : "system";
    }
}
