package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.PartnerConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerConfigRepository extends JpaRepository<PartnerConfig, Long>, JpaSpecificationExecutor<PartnerConfig> {

    List<PartnerConfig> findByPartnerId(Long partnerId);

    boolean existsByPartnerIdAndParamName(Long partnerId, String paramName);
}
