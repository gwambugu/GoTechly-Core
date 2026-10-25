package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.AmountLimits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the AmountLimits entity.
 */
@Repository
public interface AmountLimitsRepository extends JpaRepository<AmountLimits, Long>, JpaSpecificationExecutor<AmountLimits> {
    List<AmountLimits> findByChannelName(String channelName);
}
