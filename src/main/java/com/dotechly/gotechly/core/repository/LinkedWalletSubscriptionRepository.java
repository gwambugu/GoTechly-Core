package com.dotechly.gotechly.core.repository;


import com.dotechly.gotechly.core.domain.LinkedWalletSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkedWalletSubscriptionRepository
        extends JpaRepository<LinkedWalletSubscription, Long>, JpaSpecificationExecutor<LinkedWalletSubscription> {

    Optional<LinkedWalletSubscription> findFirstBySavingsAccountNumber(String savingsAccountNumber);

    List<LinkedWalletSubscription> findByMobilePhoneNumber(String mobilePhoneNumber);

    // ---- account/alias linkage lookups (LinkedAccountService) ------------------------------------
    // All scoped by paymentType: one account may legitimately be linked into more than one scheme's
    // directory, and the unscoped finder above would then return an arbitrary one of them.

    /** By the channel's correlation id — the status API's primary key. Unique in the DB. */
    Optional<LinkedWalletSubscription> findByRequestId(String requestId);

    Optional<LinkedWalletSubscription> findFirstByPaymentTypeAndSavingsAccountNumber(
            String paymentType, String savingsAccountNumber);

    Optional<LinkedWalletSubscription> findFirstByPaymentTypeAndAliasName(
            String paymentType, String aliasName);

    Optional<LinkedWalletSubscription> findFirstByPaymentTypeAndMobilePhoneNumber(
            String paymentType, String mobilePhoneNumber);

    /**
     * By the scheme's own reference. Used to correlate an asynchronous account-management callback
     * that echoes only its own reference back, not our requestId.
     */
    Optional<LinkedWalletSubscription> findFirstByPaymentTypeAndReferenceNumber(
            String paymentType, String referenceNumber);

    Optional<LinkedWalletSubscription> findFirstBySavingsAccountNumberAndMobilePhoneNumberAndVmtRefId(String savingsAccountNumber,
                                                                                                      String mobilePhoneNumber,
                                                                                                      Long vmtRefId);
}
