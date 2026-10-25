package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.EndpointConfigs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the EndpointConfigs entity.
 */
@Repository
public interface EndpointConfigsRepository extends JpaRepository<EndpointConfigs, Long>, JpaSpecificationExecutor<EndpointConfigs> {
    Optional<EndpointConfigs> findByServiceName(String serviceName);

}
