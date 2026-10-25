package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.domain.PaymentLog;
import com.dotechly.gotechly.core.repository.PaymentLogRepository;
import com.dotechly.gotechly.core.service.criteria.PaymentLogCriteria;
import com.dotechly.gotechly.core.service.dto.PaymentLogDTO;
import com.dotechly.gotechly.core.service.mapper.PaymentLogMapper;
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
public class PaymentLogQueryService {

    private final Logger log = LoggerFactory.getLogger(PaymentLogQueryService.class);

    private final PaymentLogRepository paymentLogRepository;

    private final PaymentLogMapper paymentLogMapper;

    public PaymentLogQueryService(PaymentLogRepository paymentLogRepository, PaymentLogMapper paymentLogMapper) {
        this.paymentLogRepository = paymentLogRepository;
        this.paymentLogMapper = paymentLogMapper;
    }

    @Transactional(readOnly = true)
    public List<PaymentLogDTO> findByCriteria(PaymentLogCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<PaymentLog> specification = createSpecification(criteria);
        return paymentLogMapper.toDto(paymentLogRepository.findAll(specification));
    }

    @Transactional(readOnly = true)
    public Page<PaymentLogDTO> findByCriteria(PaymentLogCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<PaymentLog> specification = createSpecification(criteria);
        return paymentLogRepository.findAll(specification, page).map(paymentLogMapper::toDto);
    }

    @Transactional(readOnly = true)
    public long countByCriteria(PaymentLogCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<PaymentLog> specification = createSpecification(criteria);
        return paymentLogRepository.count(specification);
    }

    protected Specification<PaymentLog> createSpecification(PaymentLogCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null) {
                if (criteria.getId() != null && criteria.getId().getEquals() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("id"), criteria.getId().getEquals()));
                }
                if (criteria.getPaymentType() != null && criteria.getPaymentType().getEquals() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("paymentType"), criteria.getPaymentType().getEquals()));
                }
                if (criteria.getRequestId() != null) {
                    if (criteria.getRequestId().getEquals() != null) {
                        predicates.add(criteriaBuilder.equal(root.get("requestId"), criteria.getRequestId().getEquals()));
                    } else if (criteria.getRequestId().getContains() != null) {
                        predicates.add(criteriaBuilder.like(root.get("requestId"),
                                "%" + criteria.getRequestId().getContains() + "%"));
                    }
                }
                if (criteria.getStatus() != null && criteria.getStatus().getEquals() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), criteria.getStatus().getEquals()));
                }
                if (criteria.getSourceSystem() != null) {
                    if (criteria.getSourceSystem().getEquals() != null) {
                        predicates.add(criteriaBuilder.equal(root.get("sourceSystem"), criteria.getSourceSystem().getEquals()));
                    } else if (criteria.getSourceSystem().getContains() != null) {
                        predicates.add(criteriaBuilder.like(root.get("sourceSystem"),
                                "%" + criteria.getSourceSystem().getContains() + "%"));
                    }
                }
                if (criteria.getResponseCode() != null && criteria.getResponseCode().getEquals() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("responseCode"), criteria.getResponseCode().getEquals()));
                }
                if (criteria.getApiMode() != null && criteria.getApiMode().getEquals() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("apiMode"), criteria.getApiMode().getEquals()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
