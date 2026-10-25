package com.dotechly.gotechly.core.service.partner;

import com.dotechly.gotechly.core.domain.Partner;
import com.dotechly.gotechly.core.repository.PartnerRepository;
import com.dotechly.gotechly.core.service.criteria.PartnerCriteria;
import com.dotechly.gotechly.core.service.dto.PartnerDTO;
import com.dotechly.gotechly.core.service.mapper.PartnerMapper;
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
public class PartnerQueryService {

    private final Logger log = LoggerFactory.getLogger(PartnerQueryService.class);

    private final PartnerRepository partnerRepository;
    private final PartnerMapper partnerMapper;

    public PartnerQueryService(PartnerRepository partnerRepository, PartnerMapper partnerMapper) {
        this.partnerRepository = partnerRepository;
        this.partnerMapper = partnerMapper;
    }

    public List<PartnerDTO> findByCriteria(PartnerCriteria criteria) {
        log.debug("find by criteria : {}", criteria);
        final Specification<Partner> specification = createSpecification(criteria);
        return partnerMapper.toDto(partnerRepository.findAll(specification));
    }

    public Page<PartnerDTO> findByCriteria(PartnerCriteria criteria, Pageable page) {
        log.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Partner> specification = createSpecification(criteria);
        return partnerRepository.findAll(specification, page).map(partnerMapper::toDto);
    }

    public long countByCriteria(PartnerCriteria criteria) {
        log.debug("count by criteria : {}", criteria);
        final Specification<Partner> specification = createSpecification(criteria);
        return partnerRepository.count(specification);
    }

    protected Specification<Partner> createSpecification(PartnerCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null) {
                if (criteria.getId() != null && criteria.getId().getEquals() != null) {
                    predicates.add(cb.equal(root.get("id"), criteria.getId().getEquals()));
                }
                if (criteria.getPartnerCode() != null) {
                    if (criteria.getPartnerCode().getContains() != null) {
                        predicates.add(cb.like(cb.lower(root.get("partnerCode")),
                            "%" + criteria.getPartnerCode().getContains().toLowerCase() + "%"));
                    } else if (criteria.getPartnerCode().getEquals() != null) {
                        predicates.add(cb.equal(root.get("partnerCode"), criteria.getPartnerCode().getEquals()));
                    }
                }
                if (criteria.getClientShortName() != null) {
                    if (criteria.getClientShortName().getContains() != null) {
                        predicates.add(cb.like(cb.lower(root.get("clientShortName")),
                            "%" + criteria.getClientShortName().getContains().toLowerCase() + "%"));
                    } else if (criteria.getClientShortName().getEquals() != null) {
                        predicates.add(cb.equal(root.get("clientShortName"), criteria.getClientShortName().getEquals()));
                    }
                }
                if (criteria.getLegalName() != null) {
                    if (criteria.getLegalName().getContains() != null) {
                        predicates.add(cb.like(cb.lower(root.get("legalName")),
                            "%" + criteria.getLegalName().getContains().toLowerCase() + "%"));
                    } else if (criteria.getLegalName().getEquals() != null) {
                        predicates.add(cb.equal(root.get("legalName"), criteria.getLegalName().getEquals()));
                    }
                }
                if (criteria.getAgentId() != null) {
                    if (criteria.getAgentId().getContains() != null) {
                        predicates.add(cb.like(cb.lower(root.get("agentId")),
                            "%" + criteria.getAgentId().getContains().toLowerCase() + "%"));
                    } else if (criteria.getAgentId().getEquals() != null) {
                        predicates.add(cb.equal(root.get("agentId"), criteria.getAgentId().getEquals()));
                    }
                }
                if (criteria.getPartnerType() != null && criteria.getPartnerType().getEquals() != null) {
                    predicates.add(cb.equal(root.get("partnerType"), criteria.getPartnerType().getEquals()));
                }
                if (criteria.getCountryCode() != null && criteria.getCountryCode().getEquals() != null) {
                    predicates.add(cb.equal(root.get("countryCode"), criteria.getCountryCode().getEquals()));
                }
                if (criteria.getContactEmail() != null) {
                    if (criteria.getContactEmail().getContains() != null) {
                        predicates.add(cb.like(cb.lower(root.get("contactEmail")),
                            "%" + criteria.getContactEmail().getContains().toLowerCase() + "%"));
                    } else if (criteria.getContactEmail().getEquals() != null) {
                        predicates.add(cb.equal(root.get("contactEmail"), criteria.getContactEmail().getEquals()));
                    }
                }
                if (criteria.getContactPhone() != null) {
                    if (criteria.getContactPhone().getContains() != null) {
                        predicates.add(cb.like(cb.lower(root.get("contactPhone")),
                            "%" + criteria.getContactPhone().getContains().toLowerCase() + "%"));
                    } else if (criteria.getContactPhone().getEquals() != null) {
                        predicates.add(cb.equal(root.get("contactPhone"), criteria.getContactPhone().getEquals()));
                    }
                }
                if (criteria.getStatus() != null && criteria.getStatus().getEquals() != null) {
                    predicates.add(cb.equal(root.get("status"), criteria.getStatus().getEquals()));
                }
                if (criteria.getKycStatus() != null && criteria.getKycStatus().getEquals() != null) {
                    predicates.add(cb.equal(root.get("kycStatus"), criteria.getKycStatus().getEquals()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
