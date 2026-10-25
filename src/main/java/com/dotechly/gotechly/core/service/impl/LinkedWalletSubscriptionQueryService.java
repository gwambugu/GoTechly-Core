package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.domain.LinkedWalletSubscription;
import com.dotechly.gotechly.core.repository.LinkedWalletSubscriptionRepository;
import com.dotechly.gotechly.core.service.criteria.LinkedWalletSubscriptionCriteria;
import com.dotechly.gotechly.core.service.dto.LinkedWalletSubscriptionDTO;
import com.dotechly.gotechly.core.service.filter.StringFilter;
import com.dotechly.gotechly.core.service.mapper.LinkedWalletSubscriptionMapper;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
public class LinkedWalletSubscriptionQueryService {

    private final Logger log = LoggerFactory.getLogger(LinkedWalletSubscriptionQueryService.class);

    private final LinkedWalletSubscriptionRepository repository;
    private final LinkedWalletSubscriptionMapper mapper;

    public LinkedWalletSubscriptionQueryService(LinkedWalletSubscriptionRepository repository,
                                                LinkedWalletSubscriptionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<LinkedWalletSubscriptionDTO> findByCriteria(LinkedWalletSubscriptionCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        return mapper.toDto(repository.findAll(createSpecification(criteria)));
    }

    public Page<LinkedWalletSubscriptionDTO> findByCriteria(LinkedWalletSubscriptionCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        return repository.findAll(createSpecification(criteria), page).map(mapper::toDto);
    }

    public long countByCriteria(LinkedWalletSubscriptionCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        return repository.count(createSpecification(criteria));
    }

    protected Specification<LinkedWalletSubscription> createSpecification(LinkedWalletSubscriptionCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria != null) {
                if (criteria.getId() != null && criteria.getId().getEquals() != null) {
                    predicates.add(cb.equal(root.get("id"), criteria.getId().getEquals()));
                }
                addString(predicates, cb, root, "paymentType", criteria.getPaymentType());
                addString(predicates, cb, root, "aliasName", criteria.getAliasName());
                addString(predicates, cb, root, "requestId", criteria.getRequestId());
                addString(predicates, cb, root, "serviceType", criteria.getServiceType());
                addString(predicates, cb, root, "pspName", criteria.getPspName());
                addString(predicates, cb, root, "mobilePhoneNumber", criteria.getMobilePhoneNumber());
                addString(predicates, cb, root, "savingsAccountNumber", criteria.getSavingsAccountNumber());
                addString(predicates, cb, root, "bankName", criteria.getBankName());
                addString(predicates, cb, root, "bankShortCode", criteria.getBankShortCode());
                addString(predicates, cb, root, "referenceNumber", criteria.getReferenceNumber());
                addString(predicates, cb, root, "nationalId", criteria.getNationalId());
                addString(predicates, cb, root, "agentNumber", criteria.getAgentNumber());
                addString(predicates, cb, root, "vmtRef", criteria.getVmtRef());
                addString(predicates, cb, root, "linkStatus", criteria.getLinkStatus());
                if (criteria.getStatus() != null && criteria.getStatus().getEquals() != null) {
                    predicates.add(cb.equal(root.get("status"), criteria.getStatus().getEquals()));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void addString(List<Predicate> predicates, CriteriaBuilder cb,
                           Root<LinkedWalletSubscription> root, String attr, StringFilter filter) {
        if (filter == null) return;
        if (filter.getContains() != null) {
            predicates.add(cb.like(cb.lower(root.get(attr)),
                    "%" + filter.getContains().toLowerCase() + "%"));
        } else if (filter.getEquals() != null) {
            predicates.add(cb.equal(root.get(attr), filter.getEquals()));
        }
    }
}
