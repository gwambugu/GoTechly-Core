package com.dotechly.gotechly.core.repository;

import com.dotechly.gotechly.core.domain.PaymentLog;
import com.dotechly.gotechly.spi.service.dto.db.CachedReceiverNames;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Spring Data JPA repository for the PaymentLog entity.
 */
@Repository
public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long>, JpaSpecificationExecutor<PaymentLog> {
    Optional<PaymentLog> findByRequestId(String requestId);
    Optional<PaymentLog> findFirstByRequestId(String requestId);

    /** Shared column list — payment_log, pending_payment_log and history_payment_log are created by
     *  separate Liquibase changesets, so physical column order is not guaranteed to match; names are. */
    String PAYMENT_LOG_COLUMNS = "id, payment_type, request_type, request_id, request_timestamp, amount, currency_code, charge_amount, charge_currency_code, processing_code, app_instance_id, app_host, status, api_mode, response_code, response_desc, response_timestamp, source_ip, source_system, source_system_call_back_url, free_text_1, free_text_2, hash_1, hash_2, record_version, created_at, updated_at, sender_type, sender_account, sender_currency_code, sender_business_number, sender_tran_code, sender_reference, sender_msisdn, sender_narration, sender_sender_name, sender_party_id, sender_country_code, receiver_type, receiver_account, receiver_currency_code, receiver_business_number, receiver_tran_code, receiver_reference, receiver_narration, receiver_msisdn, receiver_receiver_name, receiver_party_id, receiver_country_code, cbs_request_id, cbs_req, cbs_req_time, cbs_narration, cbs_host, cbs_dr_acc, cbs_cr_acc, transaction_code, cbs_status, cbs_api_mode, cbs_resp_code, cbs_resp_desc, cbs_tran_ref, cbs_resp_time, cbs_retry_count, cbs_rev_resp_code, cbs_rev_resp_desc, sender_additional_fields, receiver_additional_fields, details_additional_fields, cbs_additional_fields, psp_additional_fields, psp_request_id, psp_id, psp_request_time, psp_narration, psp_host_url, psp_dr_account, psp_cr_account, psp_status, psp_api_mode, psp_request, psp_response, psp_response_time, psp_response_code, psp_response_desc, psp_result, psp_result_code, psp_result_desc, psp_reference_code, psp_result_time, psp_retry_count, psp_rev_resp_code, psp_rev_resp_desc, status_timestamp, status_count, status_response_code, status_response_description";

    /**
     * Looks up a payment log by requestId across all three tables — active (payment_log),
     * pending (pending_payment_log) and history (history_payment_log). EOD archival moves and
     * deletes in a single transaction, so a requestId is never visible in more than one table
     * at a time; the result is therefore at most one row.
     */
    @Query(value = "SELECT " + PAYMENT_LOG_COLUMNS + " FROM payment_log WHERE request_id = :requestId " +
            "UNION ALL " +
            "SELECT " + PAYMENT_LOG_COLUMNS + " FROM pending_payment_log WHERE request_id = :requestId " +
            "UNION ALL " +
            "SELECT " + PAYMENT_LOG_COLUMNS + " FROM history_payment_log WHERE request_id = :requestId",
            nativeQuery = true)
    Optional<PaymentLog> findByRequestIdInAllTables(@Param("requestId") String requestId);

    /**
     * One page of ids old enough for EOD, oldest first — <b>regardless of status</b>.
     *
     * <p>Status decides the destination, not eligibility: settled rows go to
     * {@code history_payment_log}, everything else to {@code pending_payment_log}, and the two
     * predicates are exact complements so every selected row lands in exactly one of them. Selecting
     * on status here instead would leave the delete and the copies disagreeing about which rows they
     * are working on.
     *
     * <p>The ids are then the key for both copies and the delete, rather than all four statements
     * re-evaluating {@code created_at <= cutoff}. That predicate reads a column live traffic writes
     * continuously, so re-evaluation lets the copies and the delete see different row sets — and the
     * difference is rows deleted without being archived.
     */
    @Query(value = "SELECT id FROM payment_log WHERE created_at <= :cutoff "
            + "ORDER BY created_at LIMIT :batchSize", nativeQuery = true)
    List<Long> findArchivableIds(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);
    @Modifying
    @Query(value = "INSERT INTO history_payment_log (id, payment_type, request_type, request_id, request_timestamp, amount, currency_code, charge_amount, charge_currency_code, processing_code, app_instance_id, app_host, status, api_mode, response_code, response_desc, response_timestamp, source_ip, source_system, source_system_call_back_url, free_text_1, free_text_2, hash_1, hash_2, record_version, created_at, updated_at, sender_type, sender_account, sender_currency_code, sender_business_number, sender_tran_code, sender_reference, sender_msisdn, sender_narration, sender_sender_name, sender_party_id, sender_country_code, receiver_type, receiver_account, receiver_currency_code, receiver_business_number, receiver_tran_code, receiver_reference, receiver_narration, receiver_msisdn, receiver_receiver_name, receiver_party_id, receiver_country_code, cbs_request_id, cbs_req, cbs_req_time, cbs_narration, cbs_host, cbs_dr_acc, cbs_cr_acc, transaction_code, cbs_status, cbs_api_mode, cbs_resp_code, cbs_resp_desc, cbs_tran_ref, cbs_resp_time, cbs_retry_count, cbs_rev_resp_code, cbs_rev_resp_desc, sender_additional_fields, receiver_additional_fields, details_additional_fields, cbs_additional_fields, psp_additional_fields, psp_request_id, psp_id, psp_request_time, psp_narration, psp_host_url, psp_dr_account, psp_cr_account, psp_status, psp_api_mode, psp_request, psp_response, psp_response_time, psp_response_code, psp_response_desc, psp_result, psp_result_code, psp_result_desc, psp_reference_code, psp_result_time, psp_retry_count, psp_rev_resp_code, psp_rev_resp_desc, status_timestamp, status_count, status_response_code, status_response_description) " +
            "SELECT id, payment_type, request_type, request_id, request_timestamp, amount, currency_code, charge_amount, charge_currency_code, processing_code, app_instance_id, app_host, status, api_mode, response_code, response_desc, response_timestamp, source_ip, source_system, source_system_call_back_url, free_text_1, free_text_2, hash_1, hash_2, record_version, created_at, updated_at, sender_type, sender_account, sender_currency_code, sender_business_number, sender_tran_code, sender_reference, sender_msisdn, sender_narration, sender_sender_name, sender_party_id, sender_country_code, receiver_type, receiver_account, receiver_currency_code, receiver_business_number, receiver_tran_code, receiver_reference, receiver_narration, receiver_msisdn, receiver_receiver_name, receiver_party_id, receiver_country_code, cbs_request_id, cbs_req, cbs_req_time, cbs_narration, cbs_host, cbs_dr_acc, cbs_cr_acc, transaction_code, cbs_status, cbs_api_mode, cbs_resp_code, cbs_resp_desc, cbs_tran_ref, cbs_resp_time, cbs_retry_count, cbs_rev_resp_code, cbs_rev_resp_desc, sender_additional_fields, receiver_additional_fields, details_additional_fields, cbs_additional_fields, psp_additional_fields, psp_request_id, psp_id, psp_request_time, psp_narration, psp_host_url, psp_dr_account, psp_cr_account, psp_status, psp_api_mode, psp_request, psp_response, psp_response_time, psp_response_code, psp_response_desc, psp_result, psp_result_code, psp_result_desc, psp_reference_code, psp_result_time, psp_retry_count, psp_rev_resp_code, psp_rev_resp_desc, status_timestamp, status_count, status_response_code, status_response_description FROM payment_log " +
            "WHERE id IN (:ids) AND status IN ('COMPLETED', 'FAILED', 'completed', 'failed','RESOLVED', 'REJECTED')", nativeQuery = true)
    int copyToHistory(@Param("ids") List<Long> ids);

    @Modifying
    @Query(value = "INSERT INTO pending_payment_log (id, payment_type, request_type, request_id, request_timestamp, amount, currency_code, charge_amount, charge_currency_code, processing_code, app_instance_id, app_host, status, api_mode, response_code, response_desc, response_timestamp, source_ip, source_system, source_system_call_back_url, free_text_1, free_text_2, hash_1, hash_2, record_version, created_at, updated_at, sender_type, sender_account, sender_currency_code, sender_business_number, sender_tran_code, sender_reference, sender_msisdn, sender_narration, sender_sender_name, sender_party_id, sender_country_code, receiver_type, receiver_account, receiver_currency_code, receiver_business_number, receiver_tran_code, receiver_reference, receiver_narration, receiver_msisdn, receiver_receiver_name, receiver_party_id, receiver_country_code, cbs_request_id, cbs_req, cbs_req_time, cbs_narration, cbs_host, cbs_dr_acc, cbs_cr_acc, transaction_code, cbs_status, cbs_api_mode, cbs_resp_code, cbs_resp_desc, cbs_tran_ref, cbs_resp_time, cbs_retry_count, cbs_rev_resp_code, cbs_rev_resp_desc, sender_additional_fields, receiver_additional_fields, details_additional_fields, cbs_additional_fields, psp_additional_fields, psp_request_id, psp_id, psp_request_time, psp_narration, psp_host_url, psp_dr_account, psp_cr_account, psp_status, psp_api_mode, psp_request, psp_response, psp_response_time, psp_response_code, psp_response_desc, psp_result, psp_result_code, psp_result_desc, psp_reference_code, psp_result_time, psp_retry_count, psp_rev_resp_code, psp_rev_resp_desc, status_timestamp, status_count, status_response_code, status_response_description) " +
            "SELECT id,payment_type, request_type, request_id, request_timestamp, amount, currency_code, charge_amount, charge_currency_code, processing_code, app_instance_id, app_host, status, api_mode, response_code, response_desc, response_timestamp, source_ip, source_system, source_system_call_back_url, free_text_1, free_text_2, hash_1, hash_2, record_version, created_at, updated_at, sender_type, sender_account, sender_currency_code, sender_business_number, sender_tran_code, sender_reference, sender_msisdn, sender_narration, sender_sender_name, sender_party_id, sender_country_code, receiver_type, receiver_account, receiver_currency_code, receiver_business_number, receiver_tran_code, receiver_reference, receiver_narration, receiver_msisdn, receiver_receiver_name, receiver_party_id, receiver_country_code, cbs_request_id, cbs_req, cbs_req_time, cbs_narration, cbs_host, cbs_dr_acc, cbs_cr_acc, transaction_code, cbs_status, cbs_api_mode, cbs_resp_code, cbs_resp_desc, cbs_tran_ref, cbs_resp_time, cbs_retry_count, cbs_rev_resp_code, cbs_rev_resp_desc, sender_additional_fields, receiver_additional_fields, details_additional_fields, cbs_additional_fields, psp_additional_fields, psp_request_id, psp_id, psp_request_time, psp_narration, psp_host_url, psp_dr_account, psp_cr_account, psp_status, psp_api_mode, psp_request, psp_response, psp_response_time, psp_response_code, psp_response_desc, psp_result, psp_result_code, psp_result_desc, psp_reference_code, psp_result_time, psp_retry_count, psp_rev_resp_code, psp_rev_resp_desc, status_timestamp, status_count, status_response_code, status_response_description FROM payment_log " +
            "WHERE id IN (:ids) AND (status NOT IN ('COMPLETED', 'FAILED', 'completed', 'failed','RESOLVED', 'REJECTED') OR status IS NULL)", nativeQuery = true)
    int copyToPending(@Param("ids") List<Long> ids);

    /**
     * Deletes exactly the rows the two copies were given — no predicate of its own.
     *
     * <p>It used to re-evaluate {@code created_at <= cutoff}, which is a column live traffic writes
     * continuously, so the copies and the delete could see different row sets and the difference
     * would be rows deleted without ever being archived. Replaced the TRUNCATE before that, which
     * wiped in-flight traffic outright.
     */
    @Modifying
    @Query(value = "DELETE FROM payment_log WHERE id IN (:ids)", nativeQuery = true)
    int deleteByIds(@Param("ids") List<Long> ids);

    /**
     * Rows that no EOD run can ever touch, because every archival predicate is
     * {@code created_at <= :cutoff} and {@code NULL <= :cutoff} is UNKNOWN. Should be 0 —
     * {@code PaymentLog.stampCreated()} stamps the column on insert. A non-zero count means either
     * a pre-fix backlog awaiting the one-off backfill, or a writer bypassing JPA lifecycle
     * callbacks (native INSERT, bulk update). Used only to make a silent no-op EOD loud.
     */
    @Query(value = "SELECT COUNT(*) FROM payment_log WHERE created_at IS NULL", nativeQuery = true)
    long countUnarchivable();

    /**
     * Most recent transaction for a receiver MSISDN across all three tables — active (payment_log),
     * pending (pending_payment_log) and history (history_payment_log) — projecting only the three
     * columns the receiver-name cache needs. Returns empty when the latest matching transaction is
     * older than six months, or when none exists. EOD archival drains payment_log nightly, so the
     * union is required for the six-month window to be meaningful.
     */
    default Optional<CachedReceiverNames> findLatestReceiverNameByMsisdn(String receiverMsisdn) {
        Instant cutoff = Instant.now().atZone(ZoneOffset.UTC).minusMonths(6).toInstant();
        return findRecentReceiverRowsByMsisdn(receiverMsisdn, cutoff, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(row -> new CachedReceiverNames(
                        (String) row[0],
                        (String) row[1],
                        toInstant(row[2])));
    }

    @Query(value = "SELECT receiver_receiver_name, receiver_msisdn, request_timestamp FROM payment_log " +
            "WHERE receiver_msisdn = :receiverMsisdn AND status='COMPLETED' AND receiver_receiver_name is not null  AND request_timestamp >= :cutoff " +
            "UNION ALL " +
            "SELECT receiver_receiver_name, receiver_msisdn, request_timestamp FROM pending_payment_log " +
            "WHERE receiver_msisdn = :receiverMsisdn AND status='COMPLETED' AND receiver_receiver_name is not null  AND request_timestamp >= :cutoff " +
            "UNION ALL " +
            "SELECT receiver_receiver_name, receiver_msisdn, request_timestamp FROM history_payment_log " +
            "WHERE receiver_msisdn = :receiverMsisdn AND status='COMPLETED' AND receiver_receiver_name is not null AND request_timestamp >= :cutoff " +
            "ORDER BY request_timestamp DESC",
            nativeQuery = true)
    List<Object[]> findRecentReceiverRowsByMsisdn(@Param("receiverMsisdn") String receiverMsisdn,
                                                  @Param("cutoff") Instant cutoff,
                                                  Pageable pageable);

    private static Instant toInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant instant) return instant;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant();
        if (value instanceof java.time.OffsetDateTime offsetDateTime) return offsetDateTime.toInstant();
        throw new IllegalStateException("Unexpected type for request_timestamp: " + value.getClass());
    }
}
