package com.dotechly.gotechly.core.web.rest;

import com.dotechly.gotechly.core.service.impl.EndpointConfigsQueryService;
import com.dotechly.gotechly.core.service.criteria.EndpointConfigsCriteria;
import com.dotechly.gotechly.core.service.dto.EndpointConfigsDTO;
import com.dotechly.gotechly.core.service.impl.EndpointConfigsService;
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
@RequestMapping("/api/v1/configs")
public class EndpointConfigsResource {

    private final Logger log = LoggerFactory.getLogger(EndpointConfigsResource.class);

    private static final String ENTITY_NAME = "endpointConfigs";

    private final EndpointConfigsService endpointConfigsService;

    private final EndpointConfigsQueryService endpointConfigsQueryService;

    public EndpointConfigsResource(EndpointConfigsService endpointConfigsService, EndpointConfigsQueryService endpointConfigsQueryService) {
        this.endpointConfigsService = endpointConfigsService;
        this.endpointConfigsQueryService = endpointConfigsQueryService;
    }

    @PostMapping("/endpoint-configs")
    public ResponseEntity<EndpointConfigsDTO> createEndpointConfigs(@RequestBody EndpointConfigsDTO endpointConfigsDTO) throws URISyntaxException {
        log.debug("REST request to save EndpointConfigs : {}", endpointConfigsDTO);
        if (endpointConfigsDTO.getId() != null) {
            throw new BadRequestAlertException("A new endpointConfigs cannot already have an ID", ENTITY_NAME, "idexists");
        }
        EndpointConfigsDTO result = endpointConfigsService.save(endpointConfigsDTO);
        return ResponseEntity.created(new URI("/api/v1/configs/endpoint-configs/" + result.getId())).body(result);
    }

    @PutMapping("/endpoint-configs/{id}")
    public ResponseEntity<EndpointConfigsDTO> updateEndpointConfigs(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody EndpointConfigsDTO endpointConfigsDTO
    ) {
        log.debug("REST request to update EndpointConfigs : {}, {}", id, endpointConfigsDTO);
        if (endpointConfigsDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, endpointConfigsDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (endpointConfigsService.findOne(id).isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        EndpointConfigsDTO result = endpointConfigsService.save(endpointConfigsDTO);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/endpoint-configs")
    public ResponseEntity<List<EndpointConfigsDTO>> getAllEndpointConfigs(EndpointConfigsCriteria criteria, Pageable pageable) {
        log.debug("REST request to get EndpointConfigs by criteria: {}", criteria);
        Page<EndpointConfigsDTO> page = endpointConfigsQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/endpoint-configs/count")
    public ResponseEntity<Long> countEndpointConfigs(EndpointConfigsCriteria criteria) {
        log.debug("REST request to count EndpointConfigs by criteria: {}", criteria);
        return ResponseEntity.ok().body(endpointConfigsQueryService.countByCriteria(criteria));
    }

    @GetMapping("/endpoint-configs/{id}")
    public ResponseEntity<EndpointConfigsDTO> getEndpointConfigs(@PathVariable Long id) {
        log.debug("REST request to get EndpointConfigs : {}", id);
        Optional<EndpointConfigsDTO> endpointConfigsDTO = endpointConfigsService.findOne(id);
        return endpointConfigsDTO.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/endpoint-configs/{id}")
    public ResponseEntity<Void> deleteEndpointConfigs(@PathVariable Long id) {
        log.debug("REST request to delete EndpointConfigs : {}", id);
        endpointConfigsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
