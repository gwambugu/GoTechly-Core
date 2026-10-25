package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.PartnerServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerServiceRepository extends JpaRepository<PartnerServiceEntity, Long>, JpaSpecificationExecutor<PartnerServiceEntity> {

    List<PartnerServiceEntity> findByPartnerId(Long partnerId);

    boolean existsByPartnerIdAndPaymentTypeAndDirection(Long partnerId, String paymentType, String direction);
}
