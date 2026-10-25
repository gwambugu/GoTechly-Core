package com.dotechly.gotechly.core.service.account;


import com.dotechly.gotechly.core.pipeline.isolation.PluginSchedulers;
import com.dotechly.gotechly.core.config.outward.LinkedAccountRegistry;
import com.dotechly.gotechly.core.domain.LinkedWalletSubscription;
import com.dotechly.gotechly.core.repository.LinkedWalletSubscriptionRepository;
import com.dotechly.gotechly.spi.service.outward.account.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
/**
 * Orchestrates account/alias (proxy) linkage: resolve the scheme's {@link LinkedAccount} plugin,
 * call it, and record the outcome in {@code linked_wallet_subscription}.
 *
 * <h3>Rows are created only when the scheme accepts</h3>
 * A registration the scheme declines outright leaves no row — the channel gets the rejection
 * synchronously and there is nothing to poll for. A registration the scheme <em>accepts</em> (which
 * on an asynchronous scheme means {@link LinkedAccountStatus#PENDING}) does create the row, so the
 * later callback and the status API have something to resolve against.
 *
 * <h3>Delinking keeps the row</h3>
 * {@link LinkedAccountOperation#DEREGISTER} marks the row {@code DEREGISTERED} rather than deleting
 * it: the linkage history is what lets ops answer "why did payments to this alias stop", and the
 * scheme may still be holding a pending removal.
 *
 * <p>The JPA calls here are blocking and are made from inside the reactive chain, matching
 * {@code DarajaInquiryService} and the pipeline stages. The repository writes are short and each
 * runs in its own transaction.
 */
@Service
public class LinkedAccountService {

    private static final Logger LOG = LoggerFactory.getLogger(LinkedAccountService.class);

    /** Distinguishes linkage rows from the M-KESHO payment subscriptions sharing this table. */
    static final String SERVICE_TYPE = "ACCOUNT_LINKAGE";

    /** {@code action_code} is varchar(6), so the operation is stored abbreviated. */
    private static final Map<LinkedAccountOperation, String> ACTION_CODES = Map.of(
            LinkedAccountOperation.REGISTER, "REG",
            LinkedAccountOperation.UPDATE, "UPD",
            LinkedAccountOperation.DEREGISTER, "DEREG");

    private static final int REMARKS_MAX = 200;

    private final LinkedAccountRegistry registry;
    private final LinkedWalletSubscriptionRepository repository;
    private final PluginSchedulers schedulers;

    public LinkedAccountService(LinkedAccountRegistry registry,
                                LinkedWalletSubscriptionRepository repository,
                                PluginSchedulers schedulers) {
        this.registry = registry;
        this.repository = repository;
        this.schedulers = schedulers;
    }

    // ------------------------------------------------------------------ submit

    /**
     * Run an account-management operation end to end.
     *
     * @throws LinkedAccountRequestException when the request is unusable, the scheme has no plugin,
     *                                      or the operation contradicts the stored linkage
     */
    public Mono<LinkedAccountResponse> submit(LinkedAccountOperation operation, LinkedAccountRequest body) {
        LinkedAccountRequest request = validate(operation, body);
        LinkedAccount handler = resolveHandler(request.paymentType());

        // A replayed requestId returns the stored outcome instead of hitting the scheme twice —
        // an alias registration is not idempotent at the counterparty, and the unique index would
        // reject the second row anyway, after the damage was done upstream.
        Optional<LinkedWalletSubscription> replay = repository.findFirstByPaymentTypeAndSavingsAccountNumber(request.paymentType(), request.savingsAccountNumber());
        if(LinkedAccountOperation.UPDATE==operation && replay.isEmpty()){
            return Mono.just(toResponse(replay.orElseThrow(), operation));
        }
/*        if (replay.isPresent()) {
            LOG.info("[LINKED-ACCOUNT] {} replay for requestId '{}' — returning the stored outcome",
                    operation, request.requestId());
            return Mono.just(toResponse(replay.orElseThrow(), operation));
        }*/

        LinkedWalletSubscription existing = requireLinkageState(operation, request);
        LinkedAccountRequest resolved = fillPreviousAlias(request, existing);

        return call(handler, operation, resolved)
                .map(response -> persist(operation, resolved, existing, response))
                .onErrorResume(LinkedAccountException.class, e -> {
                    // The plugin could not attempt the call at all. No row: nothing is in flight at
                    // the scheme, so there is nothing for the status API to resolve later.
                    LOG.warn("[LINKED-ACCOUNT] {} for requestId '{}' not attempted: {} — {}",
                            operation, resolved.requestId(), e.getErrorCode(), e.getMessage());
                    return Mono.just(LinkedAccountResponse.failed(
                            resolved, operation, e.getErrorCode(), e.getMessage()));
                });
    }

    /**
     * Account linkage runs no pipeline, so it reaches the plugin without passing any
     * {@code Plugin*HandlerStage} — and therefore without the per-corridor bulkhead those stages
     * apply. It is wrapped here instead, because the call underneath is the same blocking client,
     * against the same scheme host, with the same timeouts as the payment corridor: left on the
     * shared scheduler it can drain the pool every other payment type depends on.
     */
    private Mono<LinkedAccountResponse> call(LinkedAccount handler, LinkedAccountOperation operation,
                                             LinkedAccountRequest request) {
        return schedulers.isolate(handler.paymentType(), switch (operation) {
            case REGISTER -> handler.register(request);
            case UPDATE -> handler.update(request);
            case DEREGISTER -> handler.deregister(request);
        });
    }

    // ------------------------------------------------------------------ callback

    /**
     * Settle a stored linkage from the scheme's asynchronous account-management callback.
     *
     * <p>Returns the parsed outcome even when no row matches: the caller is the scheme, and it must
     * get a 2xx either way or it will keep replaying. An unmatched callback is logged loudly — it
     * means our correlation keys and theirs have diverged, which is an integration bug, not traffic
     * to drop silently.
     */
    public Mono<LinkedAccountResponse> handleCallback(String paymentType, String rawBody) {
        LinkedAccount handler = resolveHandler(paymentType);
        return schedulers.isolate(paymentType, handler.handleCallback(rawBody))
                .map(parsed -> {
                    Optional<LinkedWalletSubscription> row = correlate(paymentType, parsed);
                    if (row.isEmpty()) {
                        LOG.error("[LINKED-ACCOUNT] Unmatched {} account-management callback "
                                        + "(requestId='{}', partnersRef='{}', account='{}') — no linkage row",
                                paymentType, parsed.requestId(), parsed.partnersRef(),
                                parsed.savingsAccountNumber());
                        return parsed;
                    }
                    LinkedWalletSubscription entity = row.orElseThrow();
                    LinkedAccountOperation operation = operationOf(entity);
                    applyOutcome(entity, operation, normalise(parsed, operation));
                    LinkedWalletSubscription saved = repository.save(entity);
                    LOG.info("[LINKED-ACCOUNT] {} callback settled requestId '{}' as {}",
                            paymentType, saved.getRequestId(), saved.getLinkStatus());
                    return toResponse(saved, operationOf(saved));
                });
    }

    /**
     * Rewrite a confirmation to the operation the stored linkage is actually running.
     *
     * <p>A scheme's account-management callback says "the directory change you asked for is done"; it
     * does not say <em>which</em> change (R-NDPS's acmt.006 carries only the request's reference). The
     * plugin therefore reports a confirmation as {@link LinkedAccountStatus#REGISTERED}, and the row
     * is what knows whether that means REGISTERED, UPDATED or DEREGISTERED. Without this a confirmed
     * delink would be stored as {@code REGISTERED} — the exact opposite of what happened, on the field
     * every downstream reader trusts.
     *
     * <p>Non-confirmations (PENDING, REJECTED, FAILED) mean the same thing for every operation and
     * pass through untouched.
     */
    private static LinkedAccountResponse normalise(LinkedAccountResponse parsed,
                                                   LinkedAccountOperation operation) {
        if (!isConfirmation(parsed.status())) {
            return parsed;
        }
        LinkedAccountStatus target = switch (operation) {
            case REGISTER -> LinkedAccountStatus.REGISTERED;
            case UPDATE -> LinkedAccountStatus.UPDATED;
            case DEREGISTER -> LinkedAccountStatus.DEREGISTERED;
        };
        return parsed.withStatus(target, parsed.dateLinked());
    }

    private static boolean isConfirmation(LinkedAccountStatus status) {
        return status == LinkedAccountStatus.REGISTERED
                || status == LinkedAccountStatus.UPDATED
                || status == LinkedAccountStatus.DEREGISTERED;
    }

    /**
     * requestId first (the scheme echoes it when it can), then the scheme's own reference, then the
     * account number. Ordered most to least specific: an account number can match a row from an
     * earlier linkage cycle, so it is the last resort rather than the first.
     */
    private Optional<LinkedWalletSubscription> correlate(String paymentType, LinkedAccountResponse parsed) {
        if (isPresent(parsed.requestId())) {
            Optional<LinkedWalletSubscription> byRequest = repository.findByRequestId(parsed.requestId());
            if (byRequest.isPresent()) {
                return byRequest;
            }
        }
        if (isPresent(parsed.partnersRef())) {
            Optional<LinkedWalletSubscription> byRef =
                    repository.findFirstByPaymentTypeAndReferenceNumber(paymentType, parsed.partnersRef());
            if (byRef.isPresent()) {
                return byRef;
            }
        }
        if (isPresent(parsed.savingsAccountNumber())) {
            return repository.findFirstByPaymentTypeAndSavingsAccountNumber(
                    paymentType, parsed.savingsAccountNumber());
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------ status read

    /** The linkage's current state, by the channel's correlation id. */
    @Transactional(readOnly = true)
    public Optional<LinkedAccountResponse> statusByRequestId(String requestId) {
        return repository.findByRequestId(requestId).map(e -> toResponse(e, operationOf(e)));
    }

    /** The linkage's current state, by any of the scheme-scoped identifiers a channel may hold. */
    @Transactional(readOnly = true)
    public Optional<LinkedAccountResponse> statusByIdentifier(String paymentType, String accountNumber,
                                                              String alias, String msisdn) {
        Optional<LinkedWalletSubscription> row = Optional.empty();
        if (isPresent(accountNumber)) {
            row = repository.findFirstByPaymentTypeAndSavingsAccountNumber(paymentType, accountNumber);
        }
        if (row.isEmpty() && isPresent(alias)) {
            row = repository.findFirstByPaymentTypeAndAliasName(paymentType, alias);
        }
        if (row.isEmpty() && isPresent(msisdn)) {
            row = repository.findFirstByPaymentTypeAndMobilePhoneNumber(paymentType, msisdn);
        }
        return row.map(e -> toResponse(e, operationOf(e)));
    }

    public Optional<LinkedAccountResponse> statusByPaymentTypeAndAccountNumber(String paymentType, String accountNumber) {
        Optional<LinkedWalletSubscription> row = Optional.empty();
        if (isPresent(accountNumber)) {
            row = repository.findFirstByPaymentTypeAndSavingsAccountNumber(paymentType, accountNumber);
        }
        return row.map(e -> toResponse(e, operationOf(e)));
    }

    // ------------------------------------------------------------------ persistence

    /**
     * Write the outcome. A rejected or failed registration deliberately writes nothing — see the
     * class comment. An UPDATE/DEREGISTER always has a row already, and records the attempt on it
     * even when the scheme said no, because the stored alias must not silently diverge from what the
     * scheme holds.
     */
    private LinkedAccountResponse persist(LinkedAccountOperation operation, LinkedAccountRequest request,
                                          LinkedWalletSubscription existing, LinkedAccountResponse raw) {
        // Normalise here as well as on the callback path: a plugin whose parser is operation-agnostic
        // (R-NDPS's acmt.006 answers all three operations with the same ACCP) reports a confirmation
        // as REGISTERED, and only Core knows which operation it confirms.
        LinkedAccountResponse response = normalise(raw, operation);
        if (operation == LinkedAccountOperation.REGISTER) {
            if (response.status() == LinkedAccountStatus.REJECTED
                    || response.status() == LinkedAccountStatus.FAILED) {
                LOG.info("[LINKED-ACCOUNT] REGISTER for requestId '{}' answered {} ({}) — no linkage row created",
                        request.requestId(), response.status(), response.responseCode());
                return response;
            }
            LinkedWalletSubscription entity = newSubscription(request);
            applyOutcome(entity, operation, response);
            LinkedWalletSubscription saved = repository.save(entity);
            LOG.info("[LINKED-ACCOUNT] REGISTER stored linkage id={} requestId='{}' account='{}' "
                            + "alias='{}' status={}",
                    saved.getId(), saved.getRequestId(), saved.getSavingsAccountNumber(),
                    saved.getAliasName(), saved.getLinkStatus());
            return toResponse(saved, operation);
        }

        // The requestId identifies this operation, not the original registration, so it is stamped
        // onto the row — that is what makes the status API answer about the change the caller made
        // rather than about the linkage's creation.
        existing.setRequestId(request.requestId());
        existing.setActionCode(ACTION_CODES.get(operation));
        if (operation == LinkedAccountOperation.UPDATE && isPresent(request.alias())
                && response.status() != LinkedAccountStatus.REJECTED
                && response.status() != LinkedAccountStatus.FAILED) {
            existing.setAliasName(request.alias());
        }
        applyOutcome(existing, operation, response);
        LinkedWalletSubscription saved = repository.save(existing);
        LOG.info("[LINKED-ACCOUNT] {} updated linkage id={} requestId='{}' alias='{}' status={}",
                operation, saved.getId(), saved.getRequestId(), saved.getAliasName(), saved.getLinkStatus());
        return toResponse(saved, operation);
    }

    private LinkedWalletSubscription newSubscription(LinkedAccountRequest request) {
        LinkedWalletSubscription e = new LinkedWalletSubscription();
        e.setServiceType(SERVICE_TYPE);
        e.setPaymentType(request.paymentType());
        e.setPspName(request.sourceSystem());
        e.setRequestId(request.requestId());
        e.setMobilePhoneNumber(request.mobilePhoneNumber());
        e.setSavingsAccountNumber(request.savingsAccountNumber());
        // Blank-to-null: now that the alias is optional on register, a caller can send "" or "   "
        // where they mean "no proxy". Storing that whitespace would make the row look aliased to the
        // status API and to the SPA grid, and `?alias=` would never match it back.
        e.setAliasName(blankToNull(request.alias()));
        e.setOperatingCurrencyCode(request.currencyCode());
        e.setFirstName(request.firstName());
        e.setMiddleName(request.middleName());
        e.setLastName(request.lastName());
        e.setNationalId(request.nationalId());
        e.setActionCode(ACTION_CODES.get(LinkedAccountOperation.REGISTER));
        e.setCreateDate(LocalDate.now());
        e.setAdditionalFields(new LinkedHashMap<>(request.additionalFields()));
        return e;
    }

    /**
     * Copy the scheme's answer onto the row. {@code dateLinked} is only ever set forward: a later
     * UPDATE reports its own outcome but must not restamp when the account was first linked.
     */
    private void applyOutcome(LinkedWalletSubscription entity, LinkedAccountOperation operation,
                              LinkedAccountResponse response) {
        entity.setLinkStatus(response.status().name());
        entity.setRemarks(truncate(response.responseDesc(), REMARKS_MAX));
        if (isPresent(response.partnersRef())) {
            entity.setReferenceNumber(response.partnersRef());
        }
        // Only adopt the answer's alias when the scheme did not refuse. A rejected change echoes the
        // requested alias back, and storing that would leave the row claiming an alias the scheme
        // never granted — the row is what every lookup and the next update read.
        boolean refused = response.status() == LinkedAccountStatus.REJECTED
                || response.status() == LinkedAccountStatus.FAILED;
        if (isPresent(response.alias()) && !refused
                && operation != LinkedAccountOperation.DEREGISTER) {
            entity.setAliasName(response.alias());
        }
        Instant linkedAt = response.dateLinked();
        if (linkedAt == null && response.status() == LinkedAccountStatus.REGISTERED) {
            // The scheme confirmed the linkage without dating it; the confirmation is the event.
            linkedAt = Instant.now();
        }
        if (linkedAt != null && entity.getDateLinked() == null) {
            entity.setDateLinked(linkedAt);
        }
        Map<String, String> fields = entity.getAdditionalFields() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(entity.getAdditionalFields());
        fields.putAll(response.additionalFields());
        if (isPresent(response.responseCode())) {
            fields.put("linkageResponseCode", response.responseCode());
        }
        entity.setAdditionalFields(fields);
    }

    // ------------------------------------------------------------------ mapping / validation

    private LinkedAccountResponse toResponse(LinkedWalletSubscription e, LinkedAccountOperation operation) {
        Map<String, String> fields = e.getAdditionalFields() == null
                ? Map.of() : e.getAdditionalFields();
        return new LinkedAccountResponse(
                e.getRequestId(),
                e.getPaymentType(),
                operation,
                e.getSavingsAccountNumber(),
                e.getAliasName(),
                parseStatus(e.getLinkStatus()),
                e.getDateLinked(),
                e.getReferenceNumber(),
                fields.get("linkageResponseCode"),
                e.getRemarks(),
                fields);
    }

    /**
     * An unrecognised stored status reads as PENDING rather than throwing: the row is data, the
     * enum is code, and a status API that 500s because an older row holds a retired label is worse
     * than one that reports the linkage as still in flight.
     */
    private static LinkedAccountStatus parseStatus(String stored) {
        if (stored == null || stored.isBlank()) {
            return LinkedAccountStatus.PENDING;
        }
        try {
            return LinkedAccountStatus.valueOf(stored.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            LOG.warn("[LINKED-ACCOUNT] Unrecognised stored link_status '{}' — reporting PENDING", stored);
            return LinkedAccountStatus.PENDING;
        }
    }

    private static LinkedAccountOperation operationOf(LinkedWalletSubscription e) {
        String code = e.getActionCode();
        if (code == null) {
            return LinkedAccountOperation.REGISTER;
        }
        return switch (code.trim().toUpperCase()) {
            case "UPD" -> LinkedAccountOperation.UPDATE;
            case "DEREG" -> LinkedAccountOperation.DEREGISTER;
            default -> LinkedAccountOperation.REGISTER;
        };
    }

    private LinkedAccount resolveHandler(String paymentType) {
        return registry.findByPaymentType(paymentType)
                .orElseThrow(() -> new LinkedAccountRequestException(
                        LinkedAccountRequestException.Kind.NO_HANDLER,
                        "No account-linkage plugin registered for paymentType '" + paymentType
                                + "'. Registered: " + registry.getRegisteredPaymentTypes()));
    }

    /**
     * REGISTER must not shadow a live linkage with a second row; UPDATE and DEREGISTER need one to
     * act on. Both checks are local — the scheme is authoritative and will reject a duplicate
     * anyway, but a second row for one account would make every later lookup ambiguous, including
     * the callback correlation.
     */
    private LinkedWalletSubscription requireLinkageState(LinkedAccountOperation operation,
                                                         LinkedAccountRequest request) {
        Optional<LinkedWalletSubscription> current = repository
                .findFirstByPaymentTypeAndSavingsAccountNumber(
                        request.paymentType(), request.savingsAccountNumber());

        if (operation == LinkedAccountOperation.REGISTER) {
            current.filter(e -> {
                LinkedAccountStatus status = parseStatus(e.getLinkStatus());
                return status == LinkedAccountStatus.REGISTERED
                        || status == LinkedAccountStatus.UPDATED
                        || status == LinkedAccountStatus.PENDING;
            }).ifPresent(e -> {
                throw new LinkedAccountRequestException(
                        LinkedAccountRequestException.Kind.CONFLICT,
                        "Account " + request.savingsAccountNumber() + " is already linked to "
                                + request.paymentType() + " (alias '" + e.getAliasName()
                                + "', status " + e.getLinkStatus() + "). Use update or deregister.");
            });
            return null;
        }

        return current.orElseThrow(() -> new LinkedAccountRequestException(
                LinkedAccountRequestException.Kind.NOT_FOUND,
                "No " + request.paymentType() + " linkage found for account "
                        + request.savingsAccountNumber()));
    }

    /** Supply the outgoing alias from the stored row when the caller did not send it. */
    private LinkedAccountRequest fillPreviousAlias(LinkedAccountRequest request,
                                                   LinkedWalletSubscription existing) {
        if (isPresent(request.previousAlias()) || existing == null) {
            return request;
        }
        return request.with(request.requestId(), existing.getAliasName());
    }

    private LinkedAccountRequest validate(LinkedAccountOperation operation, LinkedAccountRequest body) {
        if (body == null) {
            throw new LinkedAccountRequestException(LinkedAccountRequestException.Kind.INVALID,
                    "Request body is required");
        }
        StringBuilder missing = new StringBuilder();
        require(missing, "paymentType", body.paymentType());
        require(missing, "savingsAccountNumber", body.savingsAccountNumber());
        if (operation == LinkedAccountOperation.UPDATE) {
            // UPDATE exists to change the alias, so one with no alias has nothing to do.
            // REGISTER is deliberately different: the scheme accepts an account with no proxy (the
            // R-NDPS spec ships an explicit "with no proxy" sample) and the account still enters the
            // lookup directory addressable by number — the plugin just omits the proxy block. The
            // customer can claim an alias later via UPDATE. DEREGISTER identifies the account; the
            // alias is what is being removed, not supplied.
            require(missing, "alias", body.alias());
        }
        if (operation == LinkedAccountOperation.REGISTER) {
            require(missing, "mobilePhoneNumber", body.mobilePhoneNumber());
            require(missing, "currencyCode", body.currencyCode());
            require(missing, "firstName", body.firstName());
            require(missing, "lastName", body.lastName());
            require(missing, "nationalId", body.nationalId());
        }
        if (!missing.isEmpty()) {
            throw new LinkedAccountRequestException(LinkedAccountRequestException.Kind.INVALID,
                    "Missing required field(s): " + missing);
        }
        String requestId = isPresent(body.requestId())
                ? body.requestId().trim() : UUID.randomUUID().toString();
        return body.with(requestId, body.previousAlias());
    }

    private static void require(StringBuilder missing, String name, String value) {
        if (!isPresent(value)) {
            if (!missing.isEmpty()) {
                missing.append(", ");
            }
            missing.append(name);
        }
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        return isPresent(value) ? value.trim() : null;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }
}
