package com.dotechly.gotechly.core.service.impl;

import com.dotechly.gotechly.core.domain.EndpointConfigs;
import com.dotechly.gotechly.core.repository.EndpointConfigsRepository;
import com.dotechly.gotechly.core.service.criteria.EndpointConfigsCriteria;
import com.dotechly.gotechly.core.service.dto.EndpointConfigsDTO;
import com.dotechly.gotechly.core.service.mapper.EndpointConfigsMapper;
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
public class EndpointConfigsQueryService {

    private final Logger log = LoggerFactory.getLogger(EndpointConfigsQueryService.class);

    private final EndpointConfigsRepository endpointConfigsRepository;

    private final EndpointConfigsMapper endpointConfigsMapper;

    public EndpointConfigsQueryService(EndpointConfigsRepository endpointConfigsRepository, EndpointConfigsMapper endpointConfigsMapper) {
        this.endpointConfigsRepository = endpointConfigsRepository;
        this.endpointConfigsMapper = endpointConfigsMapper;
    }

    @Transactional(readOnly = true)
    public List<EndpointConfigsDTO> findByCriteria(EndpointConfigsCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<EndpointConfigs> specification = createSpecification(criteria);
        return endpointConfigsMapper.toDto(endpointConfigsRepository.findAll(specification));
    }

    @Transactional(readOnly = true)
    public Page<EndpointConfigsDTO> findByCriteria(EndpointConfigsCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<EndpointConfigs> specification = createSpecification(criteria);
        return endpointConfigsRepository.findAll(specification, page).map(endpointConfigsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public long countByCriteria(EndpointConfigsCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<EndpointConfigs> specification = createSpecification(criteria);
        return endpointConfigsRepository.count(specification);
    }

    protected Specification<EndpointConfigs> createSpecification(EndpointConfigsCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null) {
                if (criteria.getId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("id"), criteria.getId().getEquals()));
                }
                if (criteria.getServiceName() != null) {
                    if (criteria.getServiceName().getContains() != null) {
                        predicates.add(criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("serviceName")),
                                "%" + criteria.getServiceName().getContains().toLowerCase() + "%"));
                    } else if (criteria.getServiceName().getEquals() != null) {
                        predicates.add(criteriaBuilder.equal(root.get("serviceName"), criteria.getServiceName().getEquals()));
                    }
                }
                if (criteria.getServiceType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("serviceType"), criteria.getServiceType().getEquals()));
                }
                if (criteria.getRequestType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("requestType"), criteria.getRequestType().getEquals()));
                }
                if (criteria.getReqTemplate() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("reqTemplate"), criteria.getReqTemplate().getEquals()));
                }
                if (criteria.getEndpoint() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("endpoint"), criteria.getEndpoint().getEquals()));
                }
                if (criteria.getContentType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("contentType"), criteria.getContentType().getEquals()));
                }
                if (criteria.getAdditionalHeader1() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("additionalHeader1"), criteria.getAdditionalHeader1().getEquals()));
                }
                if (criteria.getAdditionalHeader2() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("additionalHeader2"), criteria.getAdditionalHeader2().getEquals()));
                }
                if (criteria.getAdditionalHeader3() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("additionalHeader3"), criteria.getAdditionalHeader3().getEquals()));
                }
                if (criteria.getStatusExp() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("statusExp"), criteria.getStatusExp().getEquals()));
                }
                if (criteria.getTranIDExp() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("tranIDExp"), criteria.getTranIDExp().getEquals()));
                }
                if (criteria.getErrorExp() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("errorExp"), criteria.getErrorExp().getEquals()));
                }
                if (criteria.getTimeout() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("timeout"), criteria.getTimeout().getEquals()));
                }
                if (criteria.getTimeOutExp() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("timeOutExp"), criteria.getTimeOutExp().getEquals()));
                }
                if (criteria.getProviderCode() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("providerCode"), criteria.getProviderCode().getEquals()));
                }
                if (criteria.getParam1() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("param1"), criteria.getParam1().getEquals()));
                }
                if (criteria.getParam2() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("param2"), criteria.getParam2().getEquals()));
                }
                if (criteria.getParam3() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("param3"), criteria.getParam3().getEquals()));
                }
                if (criteria.getAuthType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("authType"), criteria.getAuthType().getEquals()));
                }
                if (criteria.getModifiedBy() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("modifiedBy"), criteria.getModifiedBy().getEquals()));
                }
                if (criteria.getModifyDate() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("modifyDate"), criteria.getModifyDate().getEquals()));
                }
                if (criteria.getHm() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("hm"), criteria.getHm().getEquals()));
                }
                // Add more criteria here
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
