package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.HistoryPaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the HistoryPaymentLog entity.
 */
@Repository
public interface HistoryPaymentLogRepository extends JpaRepository<HistoryPaymentLog, Long>, JpaSpecificationExecutor<HistoryPaymentLog> {
}
