package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.domain.AmountLimits;
import com.dotechly.gotechly.core.repository.AmountLimitsRepository;
import com.dotechly.gotechly.core.service.criteria.AmountLimitsCriteria;
import com.dotechly.gotechly.core.service.dto.AmountLimitsDTO;
import com.dotechly.gotechly.core.service.filter.StringFilter;
import com.dotechly.gotechly.core.service.mapper.AmountLimitsMapper;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
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
public class AmountLimitsQueryService {

    private final Logger log = LoggerFactory.getLogger(AmountLimitsQueryService.class);

    private final AmountLimitsRepository amountLimitsRepository;

    private final AmountLimitsMapper amountLimitsMapper;

    public AmountLimitsQueryService(AmountLimitsRepository amountLimitsRepository, AmountLimitsMapper amountLimitsMapper) {
        this.amountLimitsRepository = amountLimitsRepository;
        this.amountLimitsMapper = amountLimitsMapper;
    }

    @Transactional(readOnly = true)
    public List<AmountLimitsDTO> findByCriteria(AmountLimitsCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<AmountLimits> specification = createSpecification(criteria);
        return amountLimitsMapper.toDto(amountLimitsRepository.findAll(specification));
    }

    @Transactional(readOnly = true)
    public Page<AmountLimitsDTO> findByCriteria(AmountLimitsCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<AmountLimits> specification = createSpecification(criteria);
        return amountLimitsRepository.findAll(specification, page).map(amountLimitsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public long countByCriteria(AmountLimitsCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<AmountLimits> specification = createSpecification(criteria);
        return amountLimitsRepository.count(specification);
    }

    protected Specification<AmountLimits> createSpecification(AmountLimitsCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null) {
                if (criteria.getId() != null && criteria.getId().getEquals() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("id"), criteria.getId().getEquals()));
                }
                addStringPredicate(predicates, criteriaBuilder, root.get("channelName"),  criteria.getChannelName());
                addStringPredicate(predicates, criteriaBuilder, root.get("serviceName"),  criteria.getServiceName());
                addStringPredicate(predicates, criteriaBuilder, root.get("countryCode"),  criteria.getCountryCode());
                addStringPredicate(predicates, criteriaBuilder, root.get("currencyCode"), criteria.getCurrencyCode());
                addStringPredicate(predicates, criteriaBuilder, root.get("status"),       criteria.getStatus());

                if (criteria.getTransactionLimit() != null && criteria.getTransactionLimit().getEquals() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("transactionLimit"), criteria.getTransactionLimit().getEquals()));
                }
                if (criteria.getDailyLimit() != null && criteria.getDailyLimit().getEquals() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("dailyLimit"), criteria.getDailyLimit().getEquals()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Case-insensitive contains, falling back to exact equals. No-op when filter is null. */
    private static void addStringPredicate(List<Predicate> predicates,
                                           CriteriaBuilder cb,
                                           Path<String> path,
                                           StringFilter filter) {
        if (filter == null) return;
        if (filter.getContains() != null && !filter.getContains().isBlank()) {
            predicates.add(cb.like(cb.lower(path), "%" + filter.getContains().toLowerCase() + "%"));
        } else if (filter.getEquals() != null) {
            predicates.add(cb.equal(path, filter.getEquals()));
        }
    }
}
