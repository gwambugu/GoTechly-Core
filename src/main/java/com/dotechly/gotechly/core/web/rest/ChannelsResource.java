package com.dotechly.gotechly.core.web.rest;

import com.dotechly.gotechly.core.service.impl.ChannelsQueryService;
import com.dotechly.gotechly.core.service.criteria.ChannelsCriteria;
import com.dotechly.gotechly.core.service.dto.ChannelsDTO;
import com.dotechly.gotechly.core.service.impl.ChannelsService;
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
public class ChannelsResource {

    private final Logger log = LoggerFactory.getLogger(ChannelsResource.class);

    private static final String ENTITY_NAME = "channels";

    private final ChannelsService channelsService;

    private final ChannelsQueryService channelsQueryService;

    public ChannelsResource(ChannelsService channelsService, ChannelsQueryService channelsQueryService) {
        this.channelsService = channelsService;
        this.channelsQueryService = channelsQueryService;
    }

    @PostMapping("/channels")
    public ResponseEntity<ChannelsDTO> createChannels(@RequestBody ChannelsDTO channelsDTO) throws URISyntaxException {
        log.debug("REST request to save Channels : {}", channelsDTO);
        if (channelsDTO.getId() != null) {
            throw new BadRequestAlertException("A new channels cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ChannelsDTO result = channelsService.save(channelsDTO);
        return ResponseEntity.created(new URI("/api/v1/configs/channels/" + result.getId())).body(result);
    }

    @PutMapping("/channels/{id}")
    public ResponseEntity<ChannelsDTO> updateChannels(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody ChannelsDTO channelsDTO
    ) {
        log.debug("REST request to update Channels : {}, {}", id, channelsDTO);
        if (channelsDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, channelsDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (channelsService.findOne(id).isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        ChannelsDTO result = channelsService.save(channelsDTO);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/channels")
    public ResponseEntity<List<ChannelsDTO>> getAllChannels(ChannelsCriteria criteria, Pageable pageable) {
        log.debug("REST request to get Channels by criteria: {}", criteria);
        Page<ChannelsDTO> page = channelsQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/channels/count")
    public ResponseEntity<Long> countChannels(ChannelsCriteria criteria) {
        log.debug("REST request to count Channels by criteria: {}", criteria);
        return ResponseEntity.ok().body(channelsQueryService.countByCriteria(criteria));
    }

    @GetMapping("/channels/{id}")
    public ResponseEntity<ChannelsDTO> getChannels(@PathVariable Long id) {
        log.debug("REST request to get Channels : {}", id);
        Optional<ChannelsDTO> channelsDTO = channelsService.findOne(id);
        return channelsDTO.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/channels/{id}")
    public ResponseEntity<Void> deleteChannels(@PathVariable Long id) {
        log.debug("REST request to delete Channels : {}", id);
        channelsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
