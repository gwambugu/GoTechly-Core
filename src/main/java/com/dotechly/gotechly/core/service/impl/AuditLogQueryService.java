package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.domain.AuditLog;
import com.dotechly.gotechly.core.repository.AuditLogRepository;
import com.dotechly.gotechly.core.service.criteria.AuditLogCriteria;
import com.dotechly.gotechly.core.service.dto.AuditLogDTO;
import com.dotechly.gotechly.core.service.mapper.AuditLogMapper;
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

/**
 * Service for executing complex queries for {@link AuditLog} entities in the database.
 * The main input is a {@link AuditLogCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link AuditLogDTO} or a {@link Page} of {@link AuditLogDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class AuditLogQueryService {

    private final Logger log = LoggerFactory.getLogger(AuditLogQueryService.class);

    private final AuditLogRepository auditLogRepository;

    private final AuditLogMapper auditLogMapper;

    public AuditLogQueryService(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * Return a {@link List} of {@link AuditLogDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the list of matched entities.
     */
    @Transactional(readOnly = true)
    public List<AuditLogDTO> findByCriteria(AuditLogCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<AuditLog> specification = createSpecification(criteria);
        return auditLogMapper.toDto(auditLogRepository.findAll(specification));
    }

    /**
     * Return a {@link Page} of {@link AuditLogDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the page of matched entities.
     */
    @Transactional(readOnly = true)
    public Page<AuditLogDTO> findByCriteria(AuditLogCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<AuditLog> specification = createSpecification(criteria);
        return auditLogRepository.findAll(specification, page).map(auditLogMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(AuditLogCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<AuditLog> specification = createSpecification(criteria);
        return auditLogRepository.count(specification);
    }

    /**
     * Function to convert {@link AuditLogCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<AuditLog> createSpecification(AuditLogCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria != null) {
                if (criteria.getId() != null && criteria.getId().getEquals() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("id"), criteria.getId().getEquals()));
                }
                if (criteria.getEventType() != null && criteria.getEventType().getEquals() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("eventType"), criteria.getEventType().getEquals()));
                }
                if (criteria.getEntityName() != null && criteria.getEntityName().getEquals() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("entityName"), criteria.getEntityName().getEquals()));
                }
                if (criteria.getRequestId() != null && criteria.getRequestId().getEquals() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("requestId"), criteria.getRequestId().getEquals()));
                }
                // Add more filters as needed based on AuditLogCriteria
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
