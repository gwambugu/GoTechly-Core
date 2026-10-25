package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.PartnerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerAccountRepository extends JpaRepository<PartnerAccount, Long>, JpaSpecificationExecutor<PartnerAccount> {

    List<PartnerAccount> findByPartnerId(Long partnerId);

    Optional<PartnerAccount> findByPartnerIdAndAccountNumber(Long partnerId, String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByPartnerIdAndAccountNumber(Long partnerId, String accountNumber);
}
