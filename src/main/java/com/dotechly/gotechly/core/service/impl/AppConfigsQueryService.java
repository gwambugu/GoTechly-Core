package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.domain.AppConfigs;
import com.dotechly.gotechly.core.repository.AppConfigsRepository;
import com.dotechly.gotechly.core.service.criteria.AppConfigsCriteria;
import com.dotechly.gotechly.core.service.dto.AppConfigsDTO;
import com.dotechly.gotechly.core.service.mapper.AppConfigsMapper;
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
public class AppConfigsQueryService {

    private final Logger log = LoggerFactory.getLogger(AppConfigsQueryService.class);

    private final AppConfigsRepository appConfigsRepository;

    private final AppConfigsMapper appConfigsMapper;

    public AppConfigsQueryService(AppConfigsRepository appConfigsRepository, AppConfigsMapper appConfigsMapper) {
        this.appConfigsRepository = appConfigsRepository;
        this.appConfigsMapper = appConfigsMapper;
    }

    @Transactional(readOnly = true)
    public List<AppConfigsDTO> findByCriteria(AppConfigsCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<AppConfigs> specification = createSpecification(criteria);
        return appConfigsMapper.toDto(appConfigsRepository.findAll(specification));
    }

    @Transactional(readOnly = true)
    public Page<AppConfigsDTO> findByCriteria(AppConfigsCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<AppConfigs> specification = createSpecification(criteria);
        return appConfigsRepository.findAll(specification, page).map(appConfigsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public long countByCriteria(AppConfigsCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<AppConfigs> specification = createSpecification(criteria);
        return appConfigsRepository.count(specification);
    }

    protected Specification<AppConfigs> createSpecification(AppConfigsCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null) {
                if (criteria.getId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("id"), criteria.getId().getEquals()));
                }
                if (criteria.getConfigId() != null) {
                    if (criteria.getConfigId().getContains() != null) {
                        predicates.add(criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("configId")),
                                "%" + criteria.getConfigId().getContains().toLowerCase() + "%"));
                    } else if (criteria.getConfigId().getEquals() != null) {
                        predicates.add(criteriaBuilder.equal(root.get("configId"), criteria.getConfigId().getEquals()));
                    }
                }
                if (criteria.getConfigValue() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("configValue"), criteria.getConfigValue().getEquals()));
                }
                if (criteria.getServiceType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("serviceType"), criteria.getServiceType().getEquals()));
                }
                if (criteria.getConfigStatus() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("configStatus"), criteria.getConfigStatus().getEquals()));
                }
                // Add more criteria here
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
