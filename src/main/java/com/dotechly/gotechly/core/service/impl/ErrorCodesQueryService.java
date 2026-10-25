package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.domain.ErrorCodes;
import com.dotechly.gotechly.core.repository.ErrorCodesRepository;
import com.dotechly.gotechly.core.service.criteria.ErrorCodesCriteria;
import com.dotechly.gotechly.core.service.dto.ErrorCodesDTO;
import com.dotechly.gotechly.core.service.filter.StringFilter;
import com.dotechly.gotechly.core.service.mapper.ErrorCodesMapper;
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
public class ErrorCodesQueryService {

    private final Logger log = LoggerFactory.getLogger(ErrorCodesQueryService.class);

    private final ErrorCodesRepository errorCodesRepository;

    private final ErrorCodesMapper errorCodesMapper;

    public ErrorCodesQueryService(ErrorCodesRepository errorCodesRepository, ErrorCodesMapper errorCodesMapper) {
        this.errorCodesRepository = errorCodesRepository;
        this.errorCodesMapper = errorCodesMapper;
    }

    @Transactional(readOnly = true)
    public List<ErrorCodesDTO> findByCriteria(ErrorCodesCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<ErrorCodes> specification = createSpecification(criteria);
        return errorCodesMapper.toDto(errorCodesRepository.findAll(specification));
    }

    @Transactional(readOnly = true)
    public Page<ErrorCodesDTO> findByCriteria(ErrorCodesCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<ErrorCodes> specification = createSpecification(criteria);
        return errorCodesRepository.findAll(specification, page).map(errorCodesMapper::toDto);
    }

    @Transactional(readOnly = true)
    public long countByCriteria(ErrorCodesCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<ErrorCodes> specification = createSpecification(criteria);
        return errorCodesRepository.count(specification);
    }

    protected Specification<ErrorCodes> createSpecification(ErrorCodesCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null) {
                if (criteria.getId() != null && criteria.getId().getEquals() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("id"), criteria.getId().getEquals()));
                }
                addStringPredicate(predicates, criteriaBuilder, root.get("apiName"), criteria.getApiName());
                addStringPredicate(predicates, criteriaBuilder, root.get("serviceName"), criteria.getServiceName());
                addStringPredicate(predicates, criteriaBuilder, root.get("serviceType"), criteria.getServiceType());
                addStringPredicate(predicates, criteriaBuilder, root.get("requestType"), criteria.getRequestType());
                addStringPredicate(predicates, criteriaBuilder, root.get("errorCode"), criteria.getErrorCode());
                addStringPredicate(predicates, criteriaBuilder, root.get("transactionState"), criteria.getTransactionState());
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Adds a case-insensitive contains predicate (preferred) or an exact equals predicate
     *  for the given StringFilter. No-op if the filter or both operands are null. */
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
