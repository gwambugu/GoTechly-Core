package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.PendingChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingChangeRepository extends JpaRepository<PendingChange, Long>, JpaSpecificationExecutor<PendingChange> {
}
