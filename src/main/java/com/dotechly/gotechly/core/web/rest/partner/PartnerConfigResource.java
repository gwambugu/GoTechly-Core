package com.dotechly.gotechly.core.web.rest.partner;

import com.dotechly.gotechly.core.service.dto.PartnerConfigDTO;
import com.dotechly.gotechly.core.service.partner.PartnerConfigService;
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
public class PartnerConfigResource {

    private final Logger log = LoggerFactory.getLogger(PartnerConfigResource.class);
    private static final String ENTITY_NAME = "partnerConfig";

    private final PartnerConfigService configService;

    public PartnerConfigResource(PartnerConfigService configService) {
        this.configService = configService;
    }

    @PostMapping("/configs")
    public ResponseEntity<PartnerConfigDTO> createConfig(@Valid @RequestBody PartnerConfigDTO dto) throws URISyntaxException {
        log.debug("REST request to create PartnerConfig : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new config cannot already have an ID", ENTITY_NAME, "idexists");
        }
        PartnerConfigDTO result = configService.save(dto);
        return ResponseEntity.created(new URI("/api/v1/partner/configs/" + result.getId())).body(result);
    }

    @PutMapping("/configs/{id}")
    public ResponseEntity<PartnerConfigDTO> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody PartnerConfigDTO dto) {
        log.debug("REST request to update PartnerConfig : {}, {}", id, dto);
        if (dto.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, dto.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (configService.findOne(id).isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        PartnerConfigDTO result = configService.save(dto);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/partners/{partnerId}/configs")
    public ResponseEntity<List<PartnerConfigDTO>> getConfigsByPartner(@PathVariable Long partnerId) {
        log.debug("REST request to get configs for Partner : {}", partnerId);
        return ResponseEntity.ok(configService.findByPartnerId(partnerId));
    }

    @GetMapping("/configs/{id}")
    public ResponseEntity<PartnerConfigDTO> getConfig(@PathVariable Long id) {
        log.debug("REST request to get PartnerConfig : {}", id);
        Optional<PartnerConfigDTO> dto = configService.findOne(id);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/configs/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        log.debug("REST request to delete PartnerConfig : {}", id);
        configService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
