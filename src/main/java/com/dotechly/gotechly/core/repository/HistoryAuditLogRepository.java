package com.dotechly.gotechly.core.repository;


import com.dotechly.gotechly.core.domain.HistoryAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the HistoryAuditLog entity.
 */
@Repository
public interface HistoryAuditLogRepository extends JpaRepository<HistoryAuditLog, Long>, JpaSpecificationExecutor<HistoryAuditLog> {
}
