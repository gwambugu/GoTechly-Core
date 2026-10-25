package com.dotechly.gotechly.core.repository;


import com.dotechly.gotechly.core.domain.BankCodesLookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankCodesLookupRepository extends JpaRepository<BankCodesLookup, Long>, JpaSpecificationExecutor<BankCodesLookup> {

    Optional<BankCodesLookup> findFirstByBic(String bic);
    Optional<BankCodesLookup> findFirstByBicStartingWith(String bic);
    Optional<BankCodesLookup> findFirstByBankCode(String bankCode);
    Optional<BankCodesLookup> findFirstByPic(String pic);

    List<BankCodesLookup> findByCountryCodeAndBankCode(String countryCode, String bankCode);
}
