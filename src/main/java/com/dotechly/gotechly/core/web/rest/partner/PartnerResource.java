package com.dotechly.gotechly.core.web.rest.partner;

import com.dotechly.gotechly.core.service.criteria.PartnerCriteria;
import com.dotechly.gotechly.core.service.dto.PartnerDTO;
import com.dotechly.gotechly.core.service.partner.PartnerManagementService;
import com.dotechly.gotechly.core.service.partner.PartnerQueryService;
import com.dotechly.gotechly.core.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/partner")
public class PartnerResource {

    private final Logger log = LoggerFactory.getLogger(PartnerResource.class);

    private static final String ENTITY_NAME = "partner";

    private final PartnerManagementService partnerService;
    private final PartnerQueryService partnerQueryService;

    public PartnerResource(PartnerManagementService partnerService, PartnerQueryService partnerQueryService) {
        this.partnerService = partnerService;
        this.partnerQueryService = partnerQueryService;
    }

    @PostMapping("/partners")
    public ResponseEntity<PartnerDTO> createPartner(@Valid @RequestBody PartnerDTO partnerDTO) throws URISyntaxException {
        log.debug("REST request to create Partner : {}", partnerDTO);
        if (partnerDTO.getId() != null) {
            throw new BadRequestAlertException("A new partner cannot already have an ID", ENTITY_NAME, "idexists");
        }
        validateUniqueness(partnerDTO, null);
        PartnerDTO result = partnerService.create(partnerDTO);
        return ResponseEntity.created(new URI("/api/v1/partner/partners/" + result.getId())).body(result);
    }

    @PutMapping("/partners/{id}")
    public ResponseEntity<PartnerDTO> updatePartner(
            @PathVariable(value = "id") final Long id,
            @Valid @RequestBody PartnerDTO partnerDTO) {
        log.debug("REST request to update Partner : {}, {}", id, partnerDTO);
        if (partnerDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, partnerDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (partnerService.findOne(id).isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        validateUniqueness(partnerDTO, id);
        PartnerDTO result = partnerService.update(partnerDTO);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/partners")
    public ResponseEntity<List<PartnerDTO>> getAllPartners(PartnerCriteria criteria, Pageable pageable) {
        log.debug("REST request to get Partners by criteria: {}", criteria);
        Page<PartnerDTO> page = partnerQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/partners/count")
    public ResponseEntity<Long> countPartners(PartnerCriteria criteria) {
        log.debug("REST request to count Partners by criteria: {}", criteria);
        return ResponseEntity.ok().body(partnerQueryService.countByCriteria(criteria));
    }

    @GetMapping("/partners/{id}")
    public ResponseEntity<PartnerDTO> getPartner(@PathVariable Long id) {
        log.debug("REST request to get Partner : {}", id);
        Optional<PartnerDTO> partnerDTO = partnerService.findOne(id);
        return partnerDTO.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/partners/{id}")
    public ResponseEntity<Void> deletePartner(@PathVariable Long id) {
        log.debug("REST request to delete Partner : {}", id);
        partnerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Maker-Checker workflow ----

    @PostMapping("/partners/{id}/approve")
    public ResponseEntity<PartnerDTO> approvePartner(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String checkerId = body.get("checkerId");
        String remarks = body.get("remarks");
        if (checkerId == null || checkerId.isBlank()) {
            throw new BadRequestAlertException("checkerId is required", ENTITY_NAME, "checkerrequired");
        }
        PartnerDTO result = partnerService.approve(id, checkerId, remarks);
        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/partners/{id}/reject")
    public ResponseEntity<PartnerDTO> rejectPartner(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String checkerId = body.get("checkerId");
        String remarks = body.get("remarks");
        if (checkerId == null || checkerId.isBlank()) {
            throw new BadRequestAlertException("checkerId is required", ENTITY_NAME, "checkerrequired");
        }
        if (remarks == null || remarks.isBlank()) {
            throw new BadRequestAlertException("remarks are required for rejection", ENTITY_NAME, "remarksrequired");
        }
        PartnerDTO result = partnerService.reject(id, checkerId, remarks);
        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/partners/{id}/suspend")
    public ResponseEntity<PartnerDTO> suspendPartner(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String remarks = body.get("remarks");
        if (userId == null || userId.isBlank()) {
            throw new BadRequestAlertException("userId is required", ENTITY_NAME, "userrequired");
        }
        PartnerDTO result = partnerService.suspend(id, userId, remarks);
        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/partners/{id}/activate")
    public ResponseEntity<PartnerDTO> activatePartner(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String remarks = body.get("remarks");
        if (userId == null || userId.isBlank()) {
            throw new BadRequestAlertException("userId is required", ENTITY_NAME, "userrequired");
        }
        PartnerDTO result = partnerService.activate(id, userId, remarks);
        return ResponseEntity.ok().body(result);
    }

    // ---- Uniqueness validation endpoints (for frontend real-time checks) ----

    @GetMapping("/partners/check-unique")
    public ResponseEntity<Map<String, Boolean>> checkUniqueness(
            @RequestParam(required = false) String clientShortName,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String contactEmail,
            @RequestParam(required = false) String contactPhone) {
        Map<String, Boolean> result = new java.util.LinkedHashMap<>();
        if (clientShortName != null) {
            result.put("clientShortName", partnerService.existsByClientShortName(clientShortName));
        }
        if (agentId != null) {
            result.put("agentId", partnerService.existsByAgentId(agentId));
        }
        if (contactEmail != null) {
            result.put("contactEmail", partnerService.existsByContactEmail(contactEmail));
        }
        if (contactPhone != null) {
            result.put("contactPhone", partnerService.existsByContactPhone(contactPhone));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Validates uniqueness constraints on create/update.
     * On update, excludes the current entity from the check.
     */
    private void validateUniqueness(PartnerDTO dto, Long excludeId) {
        if (dto.getClientShortName() != null) {
            if (excludeId != null) {
                partnerService.findOne(excludeId).ifPresent(existing -> {
                    if (!existing.getClientShortName().equals(dto.getClientShortName())
                            && partnerService.existsByClientShortName(dto.getClientShortName())) {
                        throw new BadRequestAlertException("Client short name already exists", ENTITY_NAME, "clientshortnameexists");
                    }
                });
            } else if (partnerService.existsByClientShortName(dto.getClientShortName())) {
                throw new BadRequestAlertException("Client short name already exists", ENTITY_NAME, "clientshortnameexists");
            }
        }
        if (dto.getAgentId() != null) {
            if (excludeId != null) {
                partnerService.findOne(excludeId).ifPresent(existing -> {
                    if (!existing.getAgentId().equals(dto.getAgentId())
                            && partnerService.existsByAgentId(dto.getAgentId())) {
                        throw new BadRequestAlertException("Agent ID already exists", ENTITY_NAME, "agentidexists");
                    }
                });
            } else if (partnerService.existsByAgentId(dto.getAgentId())) {
                throw new BadRequestAlertException("Agent ID already exists", ENTITY_NAME, "agentidexists");
            }
        }
        if (dto.getContactEmail() != null) {
            if (excludeId != null) {
                partnerService.findOne(excludeId).ifPresent(existing -> {
                    if (!existing.getContactEmail().equals(dto.getContactEmail())
                            && partnerService.existsByContactEmail(dto.getContactEmail())) {
                        throw new BadRequestAlertException("Contact email already exists", ENTITY_NAME, "emailexists");
                    }
                });
            } else if (partnerService.existsByContactEmail(dto.getContactEmail())) {
                throw new BadRequestAlertException("Contact email already exists", ENTITY_NAME, "emailexists");
            }
        }
        if (dto.getContactPhone() != null && !dto.getContactPhone().isBlank()) {
            if (excludeId != null) {
                partnerService.findOne(excludeId).ifPresent(existing -> {
                    if (!existing.getContactPhone().equals(dto.getContactPhone())
                            && partnerService.existsByContactPhone(dto.getContactPhone())) {
                        throw new BadRequestAlertException("Contact phone already exists", ENTITY_NAME, "phoneexists");
                    }
                });
            } else if (partnerService.existsByContactPhone(dto.getContactPhone())) {
                throw new BadRequestAlertException("Contact phone already exists", ENTITY_NAME, "phoneexists");
            }
        }
    }
}
