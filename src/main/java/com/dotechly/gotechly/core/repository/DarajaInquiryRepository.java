package com.dotechly.gotechly.core.repository;


import com.dotechly.gotechly.core.domain.DarajaInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DarajaInquiryRepository extends JpaRepository<DarajaInquiry, Long> {

    Optional<DarajaInquiry> findByRequestId(String requestId);

    /**
     * Correlation key for the Daraja result webhook. We submit the caller's requestId as
     * OriginatorConversationID so Daraja echoes it back on the result envelope; the webhook
     * controller looks the row up by this column.
     */
    Optional<DarajaInquiry> findByOriginatorConversationId(String originatorConversationId);
}
