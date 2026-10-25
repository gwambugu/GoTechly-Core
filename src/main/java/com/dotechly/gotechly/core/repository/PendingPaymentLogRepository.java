package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.PendingPaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PendingPaymentLog entity.
 */
@Repository
public interface PendingPaymentLogRepository extends JpaRepository<PendingPaymentLog, Long>, JpaSpecificationExecutor<PendingPaymentLog> {
}
