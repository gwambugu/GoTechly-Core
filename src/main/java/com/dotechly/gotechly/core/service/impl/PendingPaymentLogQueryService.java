package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.domain.PendingPaymentLog;
import com.dotechly.gotechly.core.repository.PendingPaymentLogRepository;
import com.dotechly.gotechly.core.service.criteria.PendingPaymentLogCriteria;
import com.dotechly.gotechly.core.service.dto.PendingPaymentLogDTO;
import com.dotechly.gotechly.core.service.mapper.PendingPaymentLogMapper;
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
public class PendingPaymentLogQueryService {

    private final Logger log = LoggerFactory.getLogger(PendingPaymentLogQueryService.class);
    private final PendingPaymentLogRepository pendingPaymentLogRepository;
    private final PendingPaymentLogMapper pendingPaymentLogMapper;

    public PendingPaymentLogQueryService(PendingPaymentLogRepository pendingPaymentLogRepository, PendingPaymentLogMapper pendingPaymentLogMapper) {
        this.pendingPaymentLogRepository = pendingPaymentLogRepository;
        this.pendingPaymentLogMapper = pendingPaymentLogMapper;
    }

    @Transactional(readOnly = true)
    public List<PendingPaymentLogDTO> findByCriteria(PendingPaymentLogCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<PendingPaymentLog> specification = createSpecification(criteria);
        return pendingPaymentLogMapper.toDto(pendingPaymentLogRepository.findAll(specification));
    }

    @Transactional(readOnly = true)
    public Page<PendingPaymentLogDTO> findByCriteria(PendingPaymentLogCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<PendingPaymentLog> specification = createSpecification(criteria);
        return pendingPaymentLogRepository.findAll(specification, page).map(pendingPaymentLogMapper::toDto);
    }

    @Transactional(readOnly = true)
    public long countByCriteria(PendingPaymentLogCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<PendingPaymentLog> specification = createSpecification(criteria);
        return pendingPaymentLogRepository.count(specification);
    }

    protected Specification<PendingPaymentLog> createSpecification(PendingPaymentLogCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria != null) {
                if (criteria.getId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("id"), criteria.getId().getEquals()));
                }
                if (criteria.getPaymentType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("paymentType"), criteria.getPaymentType().getEquals()));
                }
                if (criteria.getRequestId() != null) {
                    if (criteria.getRequestId().getContains() != null) {
                        predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("requestId")),
                            "%" + criteria.getRequestId().getContains().toLowerCase() + "%"));
                    } else if (criteria.getRequestId().getEquals() != null) {
                        predicates.add(criteriaBuilder.equal(root.get("requestId"), criteria.getRequestId().getEquals()));
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
