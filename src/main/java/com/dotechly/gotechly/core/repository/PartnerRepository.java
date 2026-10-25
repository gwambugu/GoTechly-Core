package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long>, JpaSpecificationExecutor<Partner> {

    Optional<Partner> findByClientShortName(String clientShortName);

    Optional<Partner> findByAgentId(String agentId);

    Optional<Partner> findByContactEmail(String contactEmail);

    Optional<Partner> findByContactPhone(String contactPhone);

    Optional<Partner> findByPartnerCode(String partnerCode);

    boolean existsByClientShortName(String clientShortName);

    boolean existsByAgentId(String agentId);

    boolean existsByContactEmail(String contactEmail);

    boolean existsByContactPhone(String contactPhone);

    boolean existsByPartnerCode(String partnerCode);
}
