package com.dotechly.gotechly.core.web.rest.partner;

import com.dotechly.gotechly.core.service.dto.PartnerServiceDTO;
import com.dotechly.gotechly.core.service.partner.PartnerServiceManagementService;
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
public class PartnerServiceResource {

    private final Logger log = LoggerFactory.getLogger(PartnerServiceResource.class);
    private static final String ENTITY_NAME = "partnerService";

    private final PartnerServiceManagementService serviceManagement;

    public PartnerServiceResource(PartnerServiceManagementService serviceManagement) {
        this.serviceManagement = serviceManagement;
    }

    @PostMapping("/services")
    public ResponseEntity<PartnerServiceDTO> createService(@Valid @RequestBody PartnerServiceDTO dto) throws URISyntaxException {
        log.debug("REST request to create PartnerService : {}", dto);
        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new service assignment cannot already have an ID", ENTITY_NAME, "idexists");
        }
        PartnerServiceDTO result = serviceManagement.save(dto);
        return ResponseEntity.created(new URI("/api/v1/partner/services/" + result.getId())).body(result);
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<PartnerServiceDTO> updateService(
            @PathVariable Long id,
            @Valid @RequestBody PartnerServiceDTO dto) {
        log.debug("REST request to update PartnerService : {}, {}", id, dto);
        if (dto.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, dto.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (serviceManagement.findOne(id).isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        PartnerServiceDTO result = serviceManagement.save(dto);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/partners/{partnerId}/services")
    public ResponseEntity<List<PartnerServiceDTO>> getServicesByPartner(@PathVariable Long partnerId) {
        log.debug("REST request to get services for Partner : {}", partnerId);
        return ResponseEntity.ok(serviceManagement.findByPartnerId(partnerId));
    }

    @GetMapping("/services/{id}")
    public ResponseEntity<PartnerServiceDTO> getService(@PathVariable Long id) {
        log.debug("REST request to get PartnerService : {}", id);
        Optional<PartnerServiceDTO> dto = serviceManagement.findOne(id);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        log.debug("REST request to delete PartnerService : {}", id);
        serviceManagement.delete(id);
        return ResponseEntity.noContent().build();
    }
}
