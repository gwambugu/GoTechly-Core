package com.dotechly.gotechly.core.repository;


import com.dotechly.gotechly.core.domain.TemplateLookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateLookupRepository extends JpaRepository<TemplateLookup, Long>, JpaSpecificationExecutor<TemplateLookup> {

    Optional<TemplateLookup> findByTemplateName(String templateName);
}
