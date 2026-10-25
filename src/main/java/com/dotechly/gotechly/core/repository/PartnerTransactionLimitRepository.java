package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.PartnerTransactionLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerTransactionLimitRepository extends JpaRepository<PartnerTransactionLimit, Long>, JpaSpecificationExecutor<PartnerTransactionLimit> {

    List<PartnerTransactionLimit> findByPartnerId(Long partnerId);
}
