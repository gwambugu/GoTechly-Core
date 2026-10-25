package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.domain.HistoryPaymentLog;
import com.dotechly.gotechly.core.repository.HistoryPaymentLogRepository;
import com.dotechly.gotechly.core.service.criteria.HistoryPaymentLogCriteria;
import com.dotechly.gotechly.core.service.dto.HistoryPaymentLogDTO;
import com.dotechly.gotechly.core.service.mapper.HistoryPaymentLogMapper;
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
public class HistoryPaymentLogQueryService {

    private final Logger log = LoggerFactory.getLogger(HistoryPaymentLogQueryService.class);
    private final HistoryPaymentLogRepository historyPaymentLogRepository;
    private final HistoryPaymentLogMapper historyPaymentLogMapper;

    public HistoryPaymentLogQueryService(HistoryPaymentLogRepository historyPaymentLogRepository, HistoryPaymentLogMapper historyPaymentLogMapper) {
        this.historyPaymentLogRepository = historyPaymentLogRepository;
        this.historyPaymentLogMapper = historyPaymentLogMapper;
    }

    @Transactional(readOnly = true)
    public List<HistoryPaymentLogDTO> findByCriteria(HistoryPaymentLogCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<HistoryPaymentLog> specification = createSpecification(criteria);
        return historyPaymentLogMapper.toDto(historyPaymentLogRepository.findAll(specification));
    }

    @Transactional(readOnly = true)
    public Page<HistoryPaymentLogDTO> findByCriteria(HistoryPaymentLogCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<HistoryPaymentLog> specification = createSpecification(criteria);
        return historyPaymentLogRepository.findAll(specification, page).map(historyPaymentLogMapper::toDto);
    }

    @Transactional(readOnly = true)
    public long countByCriteria(HistoryPaymentLogCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<HistoryPaymentLog> specification = createSpecification(criteria);
        return historyPaymentLogRepository.count(specification);
    }

    protected Specification<HistoryPaymentLog> createSpecification(HistoryPaymentLogCriteria criteria) {
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
