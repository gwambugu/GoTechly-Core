package com.dotechly.gotechly.core.web.rest.partner;

import com.dotechly.gotechly.core.service.dto.PartnerTransactionLimitDTO;
import com.dotechly.gotechly.core.service.partner.PartnerTransactionLimitService;
import com.dotechly.gotechly.core.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/partner")
public class PartnerTransactionLimitResource {

    private final Logger log = LoggerFactory.getLogger(PartnerTransactionLimitResource.class);
    private static final String ENTITY_NAME = "partnerTransactionLimit";

    private final PartnerTransactionLimitService limitService;

    public PartnerTransactionLimitResource(PartnerTransactionLimitService limitService) {
        this.limitService = limitService;
    }

    @PostMapping("/limits")
    public ResponseEntity<PartnerTransactionLimitDTO> createLimit(@Valid @RequestBody PartnerTransactionLimitDTO dto) throws URISyntaxException {
        log.debug("REST request to create PartnerTransactionLimit : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new limit cannot already have an ID", ENTITY_NAME, "idexists");
        }
        PartnerTransactionLimitDTO result = limitService.save(dto);
        return ResponseEntity.created(new URI("/api/v1/partner/limits/" + result.getId())).body(result);
    }

    @PutMapping("/limits/{id}")
    public ResponseEntity<PartnerTransactionLimitDTO> updateLimit(
            @PathVariable Long id,
            @Valid @RequestBody PartnerTransactionLimitDTO dto) {
        log.debug("REST request to update PartnerTransactionLimit : {}, {}", id, dto);
        if (dto.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, dto.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (limitService.findOne(id).isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        PartnerTransactionLimitDTO result = limitService.save(dto);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/partners/{partnerId}/limits")
    public ResponseEntity<List<PartnerTransactionLimitDTO>> getLimitsByPartner(@PathVariable Long partnerId) {
        log.debug("REST request to get limits for Partner : {}", partnerId);
        return ResponseEntity.ok(limitService.findByPartnerId(partnerId));
    }

    @GetMapping("/limits/{id}")
    public ResponseEntity<PartnerTransactionLimitDTO> getLimit(@PathVariable Long id) {
        log.debug("REST request to get PartnerTransactionLimit : {}", id);
        Optional<PartnerTransactionLimitDTO> dto = limitService.findOne(id);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/limits/{id}")
    public ResponseEntity<Void> deleteLimit(@PathVariable Long id) {
        log.debug("REST request to delete PartnerTransactionLimit : {}", id);
        limitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
