package com.dotechly.gotechly.core.service.impl;


import com.dotechly.gotechly.core.domain.TemplateLookup;
import com.dotechly.gotechly.core.repository.TemplateLookupRepository;
import com.dotechly.gotechly.core.service.criteria.TemplateLookupCriteria;
import com.dotechly.gotechly.core.service.dto.TemplateLookupDTO;
import com.dotechly.gotechly.core.service.mapper.TemplateLookupMapper;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TemplateLookupQueryService {

    private final Logger log = LoggerFactory.getLogger(TemplateLookupQueryService.class);

    private final TemplateLookupRepository repository;
    private final TemplateLookupMapper mapper;

    public TemplateLookupQueryService(TemplateLookupRepository repository, TemplateLookupMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<TemplateLookupDTO> findByCriteria(TemplateLookupCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<TemplateLookup> specification = createSpecification(criteria);
        return mapper.toDto(repository.findAll(specification));
    }

    public Page<TemplateLookupDTO> findByCriteria(TemplateLookupCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<TemplateLookup> specification = createSpecification(criteria);
        return repository.findAll(specification, page).map(mapper::toDto);
    }

    public long countByCriteria(TemplateLookupCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<TemplateLookup> specification = createSpecification(criteria);
        return repository.count(specification);
    }

    protected Specification<TemplateLookup> createSpecification(TemplateLookupCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria != null) {
                if (criteria.getId() != null && criteria.getId().getEquals() != null) {
                    predicates.add(cb.equal(root.get("id"), criteria.getId().getEquals()));
                }
                addString(predicates, cb, root, "templateName", criteria.getTemplateName());
                addString(predicates, cb, root, "contentType", criteria.getContentType());
                addString(predicates, cb, root, "url", criteria.getUrl());
                addString(predicates, cb, root, "providerCode", criteria.getProviderCode());
                addString(predicates, cb, root, "param1", criteria.getParam1());
                addString(predicates, cb, root, "param2", criteria.getParam2());
                addString(predicates, cb, root, "modifiedBy", criteria.getModifiedBy());
                if (criteria.getModifyDate() != null) {
                    if (criteria.getModifyDate().getEquals() != null) {
                        predicates.add(cb.equal(root.get("modifyDate"), criteria.getModifyDate().getEquals()));
                    }
                    if (criteria.getModifyDate().getGreaterThanOrEqual() != null) {
                        predicates.add(cb.greaterThanOrEqualTo(root.get("modifyDate"),
                                criteria.getModifyDate().getGreaterThanOrEqual()));
                    }
                    if (criteria.getModifyDate().getLessThanOrEqual() != null) {
                        predicates.add(cb.lessThanOrEqualTo(root.get("modifyDate"),
                                criteria.getModifyDate().getLessThanOrEqual()));
                    }
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void addString(List<Predicate> predicates,
                           jakarta.persistence.criteria.CriteriaBuilder cb,
                           jakarta.persistence.criteria.Root<TemplateLookup> root,
                           String attr,
                           com.dotechly.gotechly.core.service.filter.StringFilter filter) {
        if (filter == null) return;
        if (filter.getContains() != null) {
            predicates.add(cb.like(cb.lower(root.get(attr)),
                    "%" + filter.getContains().toLowerCase() + "%"));
        } else if (filter.getEquals() != null) {
            predicates.add(cb.equal(root.get(attr), filter.getEquals()));
        }
    }
}
