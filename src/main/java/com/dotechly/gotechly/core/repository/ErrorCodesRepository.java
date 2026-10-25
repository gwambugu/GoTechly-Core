package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.ErrorCodes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the ErrorCodes entity.
 */
@Repository
public interface ErrorCodesRepository extends JpaRepository<ErrorCodes, Long>, JpaSpecificationExecutor<ErrorCodes> {
    List<ErrorCodes> findByApiName(String apiName);
    Optional<ErrorCodes> findByApiNameAndErrorCode(String apiName,String errorCode);
    Optional<ErrorCodes> findByServiceNameAndErrorCode(String serviceName,String errorCode);
}
