package com.dotechly.gotechly.core.web.rest.partner;

import com.dotechly.gotechly.core.service.dto.PartnerAccountDTO;
import com.dotechly.gotechly.core.service.partner.PartnerAccountService;
import com.dotechly.gotechly.core.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/partner")
public class PartnerAccountResource {

    private final Logger log = LoggerFactory.getLogger(PartnerAccountResource.class);
    private static final String ENTITY_NAME = "partnerAccount";

    private final PartnerAccountService accountService;

    public PartnerAccountResource(PartnerAccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/accounts")
    public ResponseEntity<PartnerAccountDTO> createAccount(@Valid @RequestBody PartnerAccountDTO dto) throws URISyntaxException {
        log.debug("REST request to create PartnerAccount : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new account cannot already have an ID", ENTITY_NAME, "idexists");
        }
        PartnerAccountDTO result = accountService.save(dto);
        return ResponseEntity.created(new URI("/api/v1/partner/accounts/" + result.getId())).body(result);
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<PartnerAccountDTO> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody PartnerAccountDTO dto) {
        log.debug("REST request to update PartnerAccount : {}, {}", id, dto);
        if (dto.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, dto.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (accountService.findOne(id).isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        PartnerAccountDTO result = accountService.save(dto);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/partners/{partnerId}/accounts")
    public ResponseEntity<List<PartnerAccountDTO>> getAccountsByPartner(@PathVariable Long partnerId) {
        log.debug("REST request to get accounts for Partner : {}", partnerId);
        return ResponseEntity.ok(accountService.findByPartnerId(partnerId));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<PartnerAccountDTO> getAccount(@PathVariable Long id) {
        log.debug("REST request to get PartnerAccount : {}", id);
        Optional<PartnerAccountDTO> dto = accountService.findOne(id);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        log.debug("REST request to delete PartnerAccount : {}", id);
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/accounts/check-unique")
    public ResponseEntity<Map<String, Boolean>> checkAccountUniqueness(
            @RequestParam String accountNumber) {
        return ResponseEntity.ok(Map.of("accountNumber", accountService.existsByAccountNumber(accountNumber)));
    }
}
