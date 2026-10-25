package com.dotechly.gotechly.core.pipeline.stages.callback;

import com.dotechly.gotechly.core.domain.Channels;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.core.service.dto.PaymentLogDTO;
import com.dotechly.gotechly.core.service.impl.ChannelsService;
import com.dotechly.gotechly.core.service.impl.PaymentLogService;
import com.dotechly.gotechly.core.web.rest.payment.dto.ChannelPaymentResponse;
import com.dotechly.gotechly.core.web.rest.payment.dto.ChannelResponseMapper;
import com.dotechly.gotechly.spi.service.AppConfigsLookup;
import com.dotechly.gotechly.spi.service.PluginContext;
import com.dotechly.gotechly.spi.service.auth.ClientAuthMethod;
import com.dotechly.gotechly.spi.service.auth.GrantType;
import com.dotechly.gotechly.spi.service.auth.OAuth2TokenProvider;
import com.dotechly.gotechly.spi.service.auth.TokenProfile;
import com.dotechly.gotechly.spi.service.auth.TokenRequestMethod;
import com.dotechly.gotechly.spi.service.dto.db.AppConfigsSPI;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pipeline stage that sends HTTP callbacks to the source system/channel
 * that initiated the transaction.
 *
 * Flow:
 * 1. Lookup channel by (sourceSystem, paymentType)
 * 2. Resolve callback URL (transaction-level first, channel-level fallback)
 * 3. Render the Freemarker template with PaymentLogSPI data
 * 4. Dispatch based on content type (JSON, XML, SOAP)
 * 5. Fire-and-forget: log result but never halt the pipeline
 */
@Component
public class CallbackStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackStage.class);

    private static final String CONFIG_PREFIX = "callback.";

    /**
     * Context attribute under which this stage publishes what the channel answered, for a
     * downstream stage to interpret (see {@code dfs-callback-response}).
     *
     * <p>Set only on a response actually received, and cleared on entry: {@code PipelineContext}
     * {@code deriveCompensation} copies attributes into every nested compensation level, so a
     * value left behind here would be readable by a later stage whose own callback never got an
     * answer. Consumers must clear it after reading.
     */
    public static final String RESPONSE_ATTR = "callback.response";

    private final ChannelsService channelsService;
    private final CallbackTemplateRenderer templateRenderer;
    private final CallbackHttpClient httpClient;
    private final OAuth2TokenProvider tokenProvider;
    private final AppConfigsLookup appConfigsLookup;
    private final ChannelResponseMapper channelResponseMapper;
    private final PaymentLogService paymentLogService;

    /** Channels we've already warned about for missing JWT config — keeps the log tidy. */
    private final Set<String> incompleteCredsWarned = ConcurrentHashMap.newKeySet();

    public CallbackStage(ChannelsService channelsService,
                         CallbackTemplateRenderer templateRenderer,
                         CallbackHttpClient httpClient,
                         OAuth2TokenProvider tokenProvider,
                         AppConfigsLookup appConfigsLookup,
                         ChannelResponseMapper channelResponseMapper,
                         PaymentLogService paymentLogService) {
        this.channelsService = channelsService;
        this.templateRenderer = templateRenderer;
        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
        this.appConfigsLookup = appConfigsLookup;
        this.channelResponseMapper = channelResponseMapper;
        this.paymentLogService = paymentLogService;
    }

    @Override
    public String name() {
        return "callback";
    }

    @Override
    public int defaultOrder() {
        return 5000;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        return executeReactive(context, request).block();
    }

    @Override
    public Mono<PaymentLogSPI> executeReactive(PipelineContext context, PaymentLogSPI request) {
        // Never let a downstream stage read an answer from an earlier callback (see RESPONSE_ATTR).
        context.setAttribute(RESPONSE_ATTR, null);
        return Mono.fromCallable(() -> resolveCallbackDetails(context, request))
                // Channel lookup + Freemarker render block; keep them off the event loop.
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(details -> dispatchCallback(context,details, request))
                .thenReturn(request)
                .onErrorResume(ex -> {
                    LOG.warn("[CALLBACK] Error sending callback for requestId='{}', paymentType='{}': {}",
                            getRequestId(request), context.getPaymentType(), ex.getMessage(), ex);
                    return Mono.just(request);
                });
    }

    /**
     * Resolves channel, URL, and rendered body.
     * Runs on the calling thread — wrapped in Mono.fromCallable() in the reactive chain.
     */
    private CallbackDispatchDetails resolveCallbackDetails(PipelineContext context, PaymentLogSPI request) {
        String sourceSystem = request.getDetails() != null ? request.getDetails().getSourceSystem() : null;


        String channelAPIName = request.getDetails().getRequestType();// context.getPaymentType();

        if(request.getDetails().getRequestType().contains(".") && request.getDetails().getRequestType().contains("INWARD")
                || request.getDetails().getRequestType().contains(".") && request.getDetails().getRequestType().contains("REVERSAL")
        ){
            String[] parts = request.getDetails().getRequestType().split("\\.");
            if(parts.length >1){
                sourceSystem = parts[0];
                channelAPIName = parts[1];
            }
        }
        if (sourceSystem == null || sourceSystem.isBlank()) {
            Optional<PaymentLogDTO> paymentLogDTOOpt  = paymentLogService.findFirstByRequestId(request.getDetails().getRequestId());
            if(paymentLogDTOOpt.isPresent()){
                PaymentLogDTO paymentLogDTO = paymentLogDTOOpt.get();
                sourceSystem = paymentLogDTO.getSourceSystem();
                channelAPIName = paymentLogDTO.getRequestType();
            }
        }
        if (sourceSystem == null || sourceSystem.isBlank()) {
            throw new IllegalStateException("Cannot send callback: sourceSystem is null/blank");
        }
        final String sourceSystemFinal = sourceSystem;
        final String channelAPINameFinal = channelAPIName;
        Optional<Channels> channelOpt  = channelsService.findBySystemNameAndApiName(sourceSystem, channelAPIName);
        if(channelOpt.isEmpty()){
            Optional<PaymentLogDTO> paymentLogDTOOpt  = paymentLogService.findFirstByRequestId(request.getDetails().getRequestId());
            if(paymentLogDTOOpt.isPresent()){
                PaymentLogDTO paymentLogDTO = paymentLogDTOOpt.get();
                sourceSystem = paymentLogDTO.getSourceSystem();
                channelAPIName = paymentLogDTO.getRequestType();
            }
        }

        // Lookup channel
        Channels channel = channelsService.findBySystemNameAndApiName(sourceSystem, channelAPIName)
                .orElseThrow(() -> new IllegalStateException(
                        "No channel found for systemName='" + sourceSystemFinal + "', apiName='" + channelAPINameFinal + "'"));

        // Resolve callback URL — transaction-level takes priority
        String callbackUrl = resolveCallbackUrl(request, channel);
        if (callbackUrl == null || callbackUrl.isBlank()) {
            throw new IllegalStateException(
                    "No callback URL available for channel '" + channel.getChannelName() + "'");
        }

        // Validate template exists
        String template = channel.getCallbackRequestTemplate();
        if (template == null || template.isBlank()) {
            throw new IllegalStateException(
                    "No callback template configured for channel '" + channel.getChannelName() + "'");
        }

        // Render template against the channel-facing response — the same contract the synchronous
        // endpoints return, so a channel parses one shape either way.
        ChannelPaymentResponse response = channelResponseMapper.toResponse(
                request, ChannelResponseMapper.scopeFor(context.getDirection(), context.getPipelinePhase()));
        // If a plugin stored a pacs.007 reversal ID override in additionalFields["callback.pac007reversalid"]
        // (e.g. to avoid DFS AG45021 duplicate-ConversationID rejection), use the 3-arg overload so
        // both the credential encryption and ${transactionRef} use that value instead of requestId.
        // The standard 2-arg overload (requestId) is used for all other corridors.
        String conversationIdOverride = (request.getDetails() != null)
                ? request.getDetails().getAdditionalField("callback.pac007reversalid") : null;
        Map<String, Object> extraModel = (conversationIdOverride != null && !conversationIdOverride.isBlank())
                ? buildExtraModel(channel.getChannelName(), request, conversationIdOverride)
                : buildExtraModel(channel.getChannelName(), request);
        String renderedBody = templateRenderer.render(template, response, request, extraModel);

        // Resolve content type
        String contentType = channel.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/json";
        }

        LOG.info("[CALLBACK] Dispatching callback: channel='{}', url='{}', contentType='{}', " +
                        "requestId='{}', paymentType='{}'",
                channel.getChannelName(), callbackUrl, contentType,
                getRequestId(request), channelAPINameFinal);

        return new CallbackDispatchDetails(callbackUrl, renderedBody, contentType, channel);
    }

    private String resolveCallbackUrl(PaymentLogSPI request, Channels channel) {
        if (request.getDetails() != null
                && request.getDetails().getSourceSystemCallBackUrl() != null
                && !request.getDetails().getSourceSystemCallBackUrl().isBlank()) {
            LOG.debug("[CALLBACK] Using transaction-level callback URL: {}",
                    request.getDetails().getSourceSystemCallBackUrl());
            return request.getDetails().getSourceSystemCallBackUrl();
        }
        LOG.debug("[CALLBACK] Using channel-level callback URL: {}", channel.getCallbackUrl());
        return channel.getCallbackUrl();
    }

    private Mono<CallbackHttpClient.ClientHttpResponse> dispatchCallback(
            PipelineContext context , CallbackDispatchDetails details, PaymentLogSPI request) {

        return resolveHeaders(details.channel())
                .flatMap(headers -> {
                    String contentType = details.contentType().toLowerCase().trim();

                    Mono<CallbackHttpClient.ClientHttpResponse> callMono;

                    if (contentType.contains("application/json")) {
                        callMono = httpClient.postJson(details.url(), details.body(), headers, null);
                    } else if (contentType.contains("text/xml") && contentType.contains("soap")) {
                        callMono = httpClient.postSoap(details.url(), details.body(), headers, null);
                    } else if (contentType.contains("application/xml") || contentType.contains("text/xml")) {
                        callMono = httpClient.postXml(details.url(), details.body(), headers, null);
                    } else {
                        LOG.warn("[CALLBACK] Unknown content type '{}', defaulting to JSON", contentType);
                        callMono = httpClient.postJson(details.url(), details.body(), headers, null);
                    }

                    return callMono;
                })
                .doOnNext(resp -> {
                    LOG.info("[CALLBACK] Response: statusCode={}, requestId='{}', channel='{}'",
                            resp.statusCode(), getRequestId(request), details.channel().getChannelName());
                    context.setAttribute(RESPONSE_ATTR, new CallbackOutcome(
                            resp.statusCode(), resp.body(),
                            details.channel().getChannelName(), details.contentType()));
                    if (request.getDetails() != null) {
                        request.getDetails().addAdditionalField(
                                "callback.httpStatus", String.valueOf(resp.statusCode()));
                    }
                });
    }

    /**
     * Resolves all HTTP headers for a callback, including a JWT bearer token if the
     * channel is configured for it.
     * <p>
     * When {@code channel.authType = "JWT"}, OAuth2 credentials are read from
     * {@code app_configs} keyed by the channel name:
     * <ul>
     *   <li>{@code callback.<channelName>.token-url} — required</li>
     *   <li>{@code callback.<channelName>.client-id} — required</li>
     *   <li>{@code callback.<channelName>.client-secret} — required</li>
     *   <li>{@code callback.<channelName>.auth-method} — optional ({@code BASIC} default, {@code POST} alt)</li>
     *   <li>{@code callback.<channelName>.scope} — optional</li>
     * </ul>
     * If any of the three required keys is missing, the callback is sent without
     * an {@code Authorization} header (graceful fallback) and a one-time WARN is
     * logged for that channel.
     */
    private Mono<Map<String, String>> resolveHeaders(Channels channel) {
        Map<String, String> staticHeaders = buildStaticHeaders(channel);

        if (!"JWT".equalsIgnoreCase(channel.getAuthType())) {
            return Mono.just(staticHeaders);
        }

        String channelName = channel.getChannelName();
        if (channelName == null || channelName.isBlank()) {
            LOG.warn("[CALLBACK] Channel has authType=JWT but no channelName — proceeding without token");
            return Mono.just(staticHeaders);
        }

        return Mono.fromCallable(() -> fetchBearerTokenOrNull(channelName))
                .subscribeOn(Schedulers.boundedElastic())
                .map(token -> {
                    if (token == null) return staticHeaders;
                    Map<String, String> merged = new HashMap<>(staticHeaders);
                    merged.put("Authorization", "Bearer " + token);
                    return merged;
                })
                .onErrorResume(ex -> {
                    LOG.error("[CALLBACK] Failed to obtain JWT token for channel '{}': {}. " +
                            "Proceeding without token.", channelName, ex.getMessage());
                    return Mono.just(staticHeaders);
                });
    }

    /**
     * Returns a bearer token for {@code channelName}, or {@code null} if OAuth2
     * credentials are incomplete in {@code app_configs} (graceful fallback —
     * caller sends the callback without an {@code Authorization} header).
     * <p>
     * Builds and re-registers the profile on every call: idempotent because
     * {@link OAuth2TokenProvider#registerProfile} no-ops when credentials are
     * unchanged, and gives free credential rotation when ops updates a row in
     * {@code app_configs} (the profile gets replaced and a new token fetched).
     * The repeat lookups are cheap — they hit the {@code appConfigs} cache.
     */
    private String fetchBearerTokenOrNull(String channelName) {
        Optional<TokenProfile> profile = buildProfileFromChannel(channelName);
        if (profile.isEmpty()) {
            return null;
        }
        tokenProvider.registerProfile(profile.get());
        return tokenProvider.getToken(channelName);
    }

    private Optional<TokenProfile> buildProfileFromChannel(String channelName) {
        String prefix = CONFIG_PREFIX + channelName + ".";
        String tokenUrl     = readConfig(prefix + "token-url");
        String clientId     = readConfig(prefix + "client-id");
        String clientSecret = readConfig(prefix + "client-secret");

        if (tokenUrl == null || clientId == null || clientSecret == null) {
            if (incompleteCredsWarned.add(channelName)) {
                LOG.warn("[CALLBACK] Channel '{}' has authType=JWT but is missing OAuth2 config in app_configs " +
                                "(require '{}token-url', '{}client-id', '{}client-secret'). " +
                                "Sending callback without Authorization header.",
                        channelName, prefix, prefix, prefix);
            }
            return Optional.empty();
        }
        // Configs are now complete — clear the warning latch so a future incomplete
        // state will warn again.
        incompleteCredsWarned.remove(channelName);

        String authMode = readConfig(prefix + "auth-method");
        ClientAuthMethod method = "POST".equalsIgnoreCase(authMode)
                ? ClientAuthMethod.POST : ClientAuthMethod.BASIC;
        String scope = readConfig(prefix + "scope");

        String requestMethodStr = readConfig(prefix + "token-request-method");
        TokenRequestMethod requestMethod;
        if ("POST_JSON".equalsIgnoreCase(requestMethodStr)) {
            requestMethod = TokenRequestMethod.POST_JSON;
        } else if ("GET".equalsIgnoreCase(requestMethodStr)) {
            requestMethod = TokenRequestMethod.GET;
        } else {
            requestMethod = TokenRequestMethod.POST;
        }

        return Optional.of(new TokenProfile(
                channelName,
                tokenUrl, clientId, clientSecret, scope,
                GrantType.CLIENT_CREDENTIALS,
                method,
                0.8,
                Duration.ofSeconds(30),
                Duration.ofSeconds(30),
                requestMethod));
    }

    /**
     * Builds extra Freemarker model variables for channels that require credential encryption.
     *
     * <p>When {@code callback.<channelName>.encryption-key} is present in {@code app_configs},
     * the plaintext password and security-credential are encrypted with AES/GCM/NoPadding,
     * binding each ciphertext to the current transaction via the transaction reference and
     * timestamp — so a captured value cannot be replayed against a different transaction.
     *
     * <p>Variables added to the template model:
     * <ul>
     *   <li>{@code encryptedPassword} — AES/GCM ciphertext of password+txRef+timestamp</li>
     *   <li>{@code encryptedSecurityCredential} — AES/GCM ciphertext of credential+txRef+timestamp</li>
     *   <li>{@code callbackTimestamp} — current time as yyyyMMddHHmmss for the SOAP Timestamp field</li>
     * </ul>
     *
     * <p>If no encryption key is configured the map still contains {@code callbackTimestamp}
     * but the encrypted fields are absent — templates using {@code ${encryptedPassword!}}
     * render an empty string.
     */
    /**
     * Builds the Freemarker extra-model using {@code details.requestId} as the conversation ID.
     * This is the standard path for all corridors (credit, MPESA, MKESHO, …).
     */
    private Map<String, Object> buildExtraModel(String channelName, PaymentLogSPI request) {
        return buildExtraModel(channelName, request, getRequestId(request));
    }

    /**
     * Overload that accepts an explicit {@code conversationId}.
     *
     * <p>Used when a plugin needs the encryption and the SOAP {@code OriginatorConversationID}
     * to be bound to a value other than {@code details.requestId} — for example, a pacs.007
     * reversal that must not reuse the original payment's conversation ID (DFS AG45021). The plugin
     * stores the override in {@code additionalFields["callback.pac007reversalid"]} before the
     * callback stage runs; the call-site reads it and picks this overload. {@code details.requestId}
     * is never changed, so {@code db-update} always finds the correct row.
     *
     * <p>If no encryption key is configured the map still contains {@code callbackTimestamp}
     * but the encrypted fields are absent — templates using {@code ${encryptedPassword!}}
     * render an empty string.
     */
    private Map<String, Object> buildExtraModel(String channelName, PaymentLogSPI request, String conversationId) {
        Map<String, Object> extra = new HashMap<>();

        // Always expose a formatted timestamp for use in <req:Timestamp>
        // Prefer the request timestamp from PaymentLogSPI so the value matches
        // what was recorded at submission time; fall back to wall-clock only if absent.
        java.sql.Timestamp requestTimestamp = (request.getDetails() != null)
                ? request.getDetails().getRequestTimestamp() : null;
        String timestamp = (requestTimestamp != null)
                ? new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(requestTimestamp)
                : java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        extra.put("callbackTimestamp", timestamp);

        // Format settlementDate from additionalFields into yyyyMMddHHmmss for the SOAP Timestamp field.
        // Tries common input formats; falls back to the raw string if none match.
        String rawSettlementDate = (request.getDetails() != null
                && request.getDetails().getAdditionalFields() != null)
                ? request.getDetails().getAdditionalFields().get("settlementDate") : null;
        extra.put("formattedSettlementDate", formatToCompact(rawSettlementDate));

        // Transaction reference used to bind the ciphertext to this specific transaction.
        // Callers pass details.requestId (standard path) or an explicit override (reversal path).
        String transactionRef = (conversationId != null && !conversationId.isBlank())
                ? conversationId : getRequestId(request);
        extra.put("transactionRef", transactionRef);

        LOG.info("[CALLBACK-ENCRYPT] channel='{}', transactionRef='{}', timestamp='{}'",
                channelName, transactionRef, timestamp);

        String prefix = CONFIG_PREFIX + channelName + ".";
        String encryptionKey = readConfig(prefix + "encryption-key");
        LOG.info("ENCRYPTION KEY {}",encryptionKey);
        if (encryptionKey == null) {
            LOG.debug("[CALLBACK-ENCRYPT] No encryption-key configured for channel='{}' — skipping credential encryption",
                    channelName);
            return extra;
        }

        String password = readConfig(prefix + "password");
        if (password != null) {
            String encryptedPassword = CallbackEncryptionUtil.encrypt(password, transactionRef, timestamp, encryptionKey);
            LOG.info("[CALLBACK-ENCRYPT] password encrypted for channel='{}', plaintext='{}{}{}', result='{}'",
                    channelName, password, transactionRef, timestamp, encryptedPassword);
            extra.put("encryptedPassword", encryptedPassword);
        }
        String securityCredential = readConfig(prefix + "security-credential");
        if (securityCredential != null) {
            String encryptedCred = CallbackEncryptionUtil.encrypt(securityCredential, transactionRef, timestamp, encryptionKey);
            LOG.info("[CALLBACK-ENCRYPT] security-credential encrypted for channel='{}', plaintext='{}{}{}', result='{}'",
                    channelName, securityCredential, transactionRef, timestamp, encryptedCred);
            extra.put("encryptedSecurityCredential", encryptedCred);
        }
        return extra;
    }

    private String readConfig(String configId) {
        return appConfigsLookup.findByConfigId(configId)
                .map(AppConfigsSPI::getConfigValue)
                .filter(v -> !v.isBlank())
                .orElse(null);
    }

    /**
     * Extracts static HTTP headers from channel's additionalFields.
     * Convention: keys prefixed with "header." are sent as HTTP headers.
     * e.g., additionalFields = {"header.X-Api-Key": "abc123"} → X-Api-Key: abc123
     */
    private Map<String, String> buildStaticHeaders(Channels channel) {
        Map<String, String> headers = new HashMap<>();
        if (channel.getAdditionalFields() != null) {
            channel.getAdditionalFields().forEach((key, value) -> {
                if (key.startsWith("header.")) {
                    headers.put(key.substring("header.".length()), value);
                }
            });
        }
        return headers;
    }

    private String getRequestId(PaymentLogSPI request) {
        return request.getDetails() != null ? request.getDetails().getRequestId() : "unknown";
    }

    /**
     * Converts a date/time string to {@code yyyyMMddHHmmss}.
     * Attempts the most common input formats seen in additionalFields:
     * <ol>
     *   <li>{@code yyyyMMddHHmmss} — already correct, returned as-is</li>
     *   <li>{@code yyyy-MM-dd HH:mm:ss}</li>
     *   <li>{@code yyyy-MM-dd'T'HH:mm:ss}</li>
     *   <li>{@code yyyy-MM-dd} — time portion defaults to 000000</li>
     * </ol>
     * Returns the raw string unchanged if it is blank or no format matches,
     * so Freemarker's {@code !} default operator can handle the empty case gracefully.
     */
    private static String formatToCompact(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String trimmed = raw.trim();
        // Already in target format
        if (trimmed.matches("\\d{14}")) return trimmed;
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                java.util.Date parsed = new java.text.SimpleDateFormat(pattern).parse(trimmed);
                return new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(parsed);
            } catch (java.text.ParseException ignored) {
                // try next
            }
        }
        LOG.warn("[CALLBACK] Could not parse settlementDate '{}' into yyyyMMddHHmmss — using raw value", trimmed);
        return trimmed;
    }

    private record CallbackDispatchDetails(
            String url,
            String body,
            String contentType,
            Channels channel
    ) {}
}
