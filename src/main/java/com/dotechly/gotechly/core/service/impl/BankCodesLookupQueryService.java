package com.dotechly.gotechly.core.service.impl;


import com.dotechly.gotechly.core.domain.BankCodesLookup;
import com.dotechly.gotechly.core.repository.BankCodesLookupRepository;
import com.dotechly.gotechly.core.service.criteria.BankCodesLookupCriteria;
import com.dotechly.gotechly.core.service.dto.BankCodesLookupDTO;
import com.dotechly.gotechly.core.service.filter.StringFilter;
import com.dotechly.gotechly.core.service.mapper.BankCodesLookupMapper;
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
public class BankCodesLookupQueryService {

    private final Logger log = LoggerFactory.getLogger(BankCodesLookupQueryService.class);

    private final BankCodesLookupRepository repository;
    private final BankCodesLookupMapper mapper;

    public BankCodesLookupQueryService(BankCodesLookupRepository repository, BankCodesLookupMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<BankCodesLookupDTO> findByCriteria(BankCodesLookupCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        return mapper.toDto(repository.findAll(createSpecification(criteria)));
    }

    public Page<BankCodesLookupDTO> findByCriteria(BankCodesLookupCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        return repository.findAll(createSpecification(criteria), page).map(mapper::toDto);
    }

    public long countByCriteria(BankCodesLookupCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        return repository.count(createSpecification(criteria));
    }

    protected Specification<BankCodesLookup> createSpecification(BankCodesLookupCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria != null) {
                if (criteria.getId() != null && criteria.getId().getEquals() != null) {
                    predicates.add(cb.equal(root.get("id"), criteria.getId().getEquals()));
                }
                addString(predicates, cb, root, "countryCode", criteria.getCountryCode());
                addString(predicates, cb, root, "bankCode", criteria.getBankCode());
                addString(predicates, cb, root, "bankName", criteria.getBankName());
                addString(predicates, cb, root, "bankShortName", criteria.getBankShortName());
                addString(predicates, cb, root, "bankBranchCode", criteria.getBankBranchCode());
                addString(predicates, cb, root, "bic", criteria.getBic());
                addString(predicates, cb, root, "pic", criteria.getPic());
                addString(predicates, cb, root, "bin", criteria.getBin());
                addString(predicates, cb, root, "status", criteria.getStatus());
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void addString(List<Predicate> predicates, CriteriaBuilder cb,
                           Root<BankCodesLookup> root, String attr, StringFilter filter) {
        if (filter == null) return;
        if (filter.getContains() != null) {
            predicates.add(cb.like(cb.lower(root.get(attr)),
                    "%" + filter.getContains().toLowerCase() + "%"));
        } else if (filter.getEquals() != null) {
            predicates.add(cb.equal(root.get(attr), filter.getEquals()));
        }
    }
}
