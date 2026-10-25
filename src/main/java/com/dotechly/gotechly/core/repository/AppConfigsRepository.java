package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.AppConfigs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the AppConfigs entity.
 */
@Repository
public interface AppConfigsRepository extends JpaRepository<AppConfigs, Long>, JpaSpecificationExecutor<AppConfigs> {
   Optional<AppConfigs> findByConfigId(String configId);
   List<AppConfigs> findByConfigIdStartingWith(String prefix);
}
