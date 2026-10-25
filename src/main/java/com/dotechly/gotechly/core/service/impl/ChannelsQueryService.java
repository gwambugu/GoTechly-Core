package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.domain.Channels;
import com.dotechly.gotechly.core.repository.ChannelsRepository;
import com.dotechly.gotechly.core.service.criteria.ChannelsCriteria;
import com.dotechly.gotechly.core.service.dto.ChannelsDTO;
import com.dotechly.gotechly.core.service.mapper.ChannelsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ChannelsQueryService {

    private final Logger log = LoggerFactory.getLogger(ChannelsQueryService.class);

    private final ChannelsRepository channelsRepository;

    private final ChannelsMapper channelsMapper;

    public ChannelsQueryService(ChannelsRepository channelsRepository, ChannelsMapper channelsMapper) {
        this.channelsRepository = channelsRepository;
        this.channelsMapper = channelsMapper;
    }

    @Transactional(readOnly = true)
    public List<ChannelsDTO> findByCriteria(ChannelsCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<Channels> specification = createSpecification(criteria);
        return channelsMapper.toDto(channelsRepository.findAll(specification));
    }

    @Transactional(readOnly = true)
    public Page<ChannelsDTO> findByCriteria(ChannelsCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Channels> specification = createSpecification(criteria);
        return channelsRepository.findAll(specification, page).map(channelsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public long countByCriteria(ChannelsCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<Channels> specification = createSpecification(criteria);
        return channelsRepository.count(specification);
    }

    protected Specification<Channels> createSpecification(ChannelsCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null) {
                if (criteria.getId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("id"), criteria.getId().getEquals()));
                }
                if (criteria.getChannelName() != null) {
                    if (criteria.getChannelName().getContains() != null) {
                        predicates.add(criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("channelName")),
                                "%" + criteria.getChannelName().getContains().toLowerCase() + "%"));
                    } else if (criteria.getChannelName().getEquals() != null) {
                        predicates.add(criteriaBuilder.equal(root.get("channelName"), criteria.getChannelName().getEquals()));
                    }
                }
                if (criteria.getSystemName() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("systemName"), criteria.getSystemName().getEquals()));
                }
                if (criteria.getApiName() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("apiName"), criteria.getApiName().getEquals()));
                }
                if (criteria.getCallbackUrl() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("callbackUrl"), criteria.getCallbackUrl().getEquals()));
                }
                if (criteria.getContentType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("contentType"), criteria.getContentType().getEquals()));
                }
                if (criteria.getAuthType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("authType"), criteria.getAuthType().getEquals()));
                }
                if (criteria.getSourceIps() != null) {
                    if (criteria.getSourceIps().getContains() != null) {
                        predicates.add(criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("sourceIps")),
                                "%" + criteria.getSourceIps().getContains().toLowerCase() + "%"));
                    } else if (criteria.getSourceIps().getEquals() != null) {
                        predicates.add(criteriaBuilder.equal(root.get("sourceIps"), criteria.getSourceIps().getEquals()));
                    }
                }
                if (criteria.getStatus() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), criteria.getStatus().getEquals()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
