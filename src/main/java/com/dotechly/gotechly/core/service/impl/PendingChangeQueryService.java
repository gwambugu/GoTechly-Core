package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.domain.PendingChange;
import com.dotechly.gotechly.core.repository.PendingChangeRepository;
import com.dotechly.gotechly.core.service.criteria.PendingChangeCriteria;
import com.dotechly.gotechly.core.service.dto.PendingChangeDTO;
import com.dotechly.gotechly.core.service.mapper.PendingChangeMapper;
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
public class PendingChangeQueryService {

    private final Logger log = LoggerFactory.getLogger(PendingChangeQueryService.class);

    private final PendingChangeRepository repository;
    private final PendingChangeMapper mapper;

    public PendingChangeQueryService(PendingChangeRepository repository, PendingChangeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<PendingChangeDTO> findByCriteria(PendingChangeCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<PendingChange> specification = createSpecification(criteria);
        return repository.findAll(specification, page).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public long countByCriteria(PendingChangeCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<PendingChange> specification = createSpecification(criteria);
        return repository.count(specification);
    }

    protected Specification<PendingChange> createSpecification(PendingChangeCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null) {
                if (criteria.getId() != null && criteria.getId().getEquals() != null) {
                    predicates.add(cb.equal(root.get("id"), criteria.getId().getEquals()));
                }
                if (criteria.getEntityType() != null && criteria.getEntityType().getEquals() != null) {
                    predicates.add(cb.equal(root.get("entityType"), criteria.getEntityType().getEquals()));
                }
                if (criteria.getChangeType() != null && criteria.getChangeType().getEquals() != null) {
                    predicates.add(cb.equal(root.get("changeType"), criteria.getChangeType().getEquals()));
                }
                if (criteria.getStatus() != null && criteria.getStatus().getEquals() != null) {
                    predicates.add(cb.equal(root.get("status"), criteria.getStatus().getEquals()));
                }
                if (criteria.getMakerUsername() != null) {
                    if (criteria.getMakerUsername().getEquals() != null) {
                        predicates.add(cb.equal(root.get("makerUsername"), criteria.getMakerUsername().getEquals()));
                    }
                    if (criteria.getMakerUsername().getContains() != null) {
                        predicates.add(cb.like(cb.lower(root.get("makerUsername")),
                                "%" + criteria.getMakerUsername().getContains().toLowerCase() + "%"));
                    }
                }
                if (criteria.getCheckerUsername() != null && criteria.getCheckerUsername().getEquals() != null) {
                    predicates.add(cb.equal(root.get("checkerUsername"), criteria.getCheckerUsername().getEquals()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
