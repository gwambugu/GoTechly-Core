package com.dotechly.gotechly.core.pipeline.stages.notification;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.core.pipeline.stages.callback.CallbackHttpClient;
import com.dotechly.gotechly.core.pipeline.stages.callback.CallbackTemplateRenderer;
import com.dotechly.gotechly.core.service.dto.TemplateLookupDTO;
import com.dotechly.gotechly.core.service.impl.TemplateLookupService;
import com.dotechly.gotechly.core.web.rest.payment.dto.ChannelPaymentResponse;
import com.dotechly.gotechly.core.web.rest.payment.dto.ChannelResponseMapper;
import com.dotechly.gotechly.spi.service.AppConfigsLookup;
import com.dotechly.gotechly.spi.service.auth.ClientAuthMethod;
import com.dotechly.gotechly.spi.service.auth.GrantType;
import com.dotechly.gotechly.spi.service.auth.OAuth2TokenProvider;
import com.dotechly.gotechly.spi.service.auth.TokenProfile;
import com.dotechly.gotechly.spi.service.auth.TokenRequestMethod;
import com.dotechly.gotechly.spi.service.dto.db.AppConfigsSPI;
import com.dotechly.gotechly.spi.service.dto.payment.Details;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends a customer notification (SMS or e-mail) for the transaction by rendering a template from
 * {@code template_lookup} and POSTing it to the endpoint that same row names.
 *
 * <p>Flow, per channel:
 * <ol>
 *   <li>Resolve the template name — {@code Details.additionalFields[<overrideKey>]} if a plugin set
 *       one, else the convention {@code <paymentType>-<SMS|EMAIL>}.</li>
 *   <li>Load the {@code template_lookup} row (cached).</li>
 *   <li>Render {@code req_template} with Freemarker against the same model the callback templates
 *       see, so one set of variable names covers both.</li>
 *   <li>Resolve the headers, swapping a bare {@code Authorization: Bearer} placeholder for a live
 *       OAuth2 token — see {@link #applyBearerToken}.</li>
 *   <li>POST to {@code url}, dispatching on {@code content_type} (JSON / XML / SOAP).</li>
 *   <li>Record the outcome on {@code Details.additionalFields}.</li>
 * </ol>
 *
 * <p><strong>Fire-and-forget, like {@code callback}.</strong> A notification is a courtesy message,
 * not part of settlement: a missing template row, an unreachable SMS gateway or a 500 from it must
 * never halt a pipeline that has already moved money. Every failure path records a reason and
 * returns the request unchanged. The one thing this stage does <em>not</em> do is stay silent —
 * every skip and every non-2xx lands in {@code additionalFields} so the transaction record itself
 * says whether the customer was told.
 *
 * <p>Deliberately reuses {@code callback}'s {@link CallbackHttpClient} and
 * {@link CallbackTemplateRenderer}: same fire-and-forget traffic shape, same Freemarker model, and
 * the renderer's compiled-template cache is keyed on template content, so the two stages share it
 * rather than each paying the parse.
 *
 * @see NotificationChannel
 */
public abstract class AbstractNotificationStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNotificationStage.class);

    private static final String DEFAULT_CONTENT_TYPE = "application/json";
    private static final String HEADER_PREFIX = "header.";

    /**
     * Namespace for a template's OAuth2 credentials in {@code app_configs}, mirroring
     * {@code callback.<channelName>.*}. Doubles as the {@code profileId} prefix, so a notification
     * profile can never collide with a callback channel's in the shared token registry.
     */
    private static final String CONFIG_PREFIX = "notification.";

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer";

    /**
     * Cap on the provider response stored on the transaction. {@code additionalFields} is a JSON
     * CLOB shared by every stage; an SMS gateway that answers with an HTML error page would
     * otherwise push a few KB of markup into every notified transaction's row.
     */
    private static final int MAX_RECORDED_RESPONSE = 500;

    private final TemplateLookupService templateLookupService;
    private final CallbackTemplateRenderer templateRenderer;
    private final CallbackHttpClient httpClient;
    private final ChannelResponseMapper channelResponseMapper;
    private final OAuth2TokenProvider tokenProvider;
    private final AppConfigsLookup appConfigsLookup;

    /**
     * Templates already warned about for incomplete OAuth2 config, so the WARN fires once per
     * template rather than once per notification. An entry is cleared as soon as that template's
     * config becomes complete, so a later regression warns again.
     */
    private final Set<String> incompleteCredsWarned = ConcurrentHashMap.newKeySet();

    protected AbstractNotificationStage(TemplateLookupService templateLookupService,
                                        CallbackTemplateRenderer templateRenderer,
                                        CallbackHttpClient httpClient,
                                        ChannelResponseMapper channelResponseMapper,
                                        OAuth2TokenProvider tokenProvider,
                                        AppConfigsLookup appConfigsLookup) {
        this.templateLookupService = templateLookupService;
        this.templateRenderer = templateRenderer;
        this.httpClient = httpClient;
        this.channelResponseMapper = channelResponseMapper;
        this.tokenProvider = tokenProvider;
        this.appConfigsLookup = appConfigsLookup;
    }

    /** Which channel this stage serves; everything that differs between SMS and e-mail lives there. */
    protected abstract NotificationChannel channel();

    @Override
    public String name() {
        return channel().stageName();
    }

    @Override
    public int defaultOrder() {
        return channel().defaultOrder();
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        // Legal only because executeReactive offloads before anything blocks — Reactor forbids
        // block() on an event loop.
        return executeReactive(context, request).block();
    }

    @Override
    public Mono<PaymentLogSPI> executeReactive(PipelineContext context, PaymentLogSPI request) {
        return Mono.fromCallable(() -> resolve(context, request))
                // Template lookup (JDBC) and the Freemarker render both block; keep them off the
                // Netty event loop. An empty Mono here — nothing to send — still falls through to
                // thenReturn, which is how a skip leaves the pipeline untouched.
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(dispatch -> send(dispatch, request))
                .thenReturn(request)
                .onErrorResume(ex -> {
                    LOG.warn("[{}] Error sending notification for requestId='{}', paymentType='{}': {}",
                            name().toUpperCase(Locale.ROOT), requestId(request),
                            context.getPaymentType(), ex.getMessage(), ex);
                    record(request, "FAILED", null, ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    return Mono.just(request);
                });
    }

    // ------------------------------------------------------------------------------- resolution

    /**
     * @return what to send, or {@code null} when this transaction has no notification configured —
     *         a normal outcome, not an error
     */
    private NotificationDispatch resolve(PipelineContext context, PaymentLogSPI request) {
        Details details = request.getDetails();
        if (details == null) {
            LOG.debug("[{}] No Details on the payment log; nothing to notify", name());
            return null;
        }

        String templateName = resolveTemplateName(context, details);
        if (isBlank(templateName)) {
            // No paymentType and no override: there is no name to look up. Not worth a WARN — it
            // means the stage was configured onto a phase whose log carries no payment type.
            LOG.debug("[{}] Cannot derive a template name for requestId='{}'", name(), requestId(request));
            return null;
        }

        Optional<TemplateLookupDTO> found = templateLookupService.findByTemplateName(templateName);
        if (found.isEmpty()) {
            skip(request, templateName, "no template_lookup row named '" + templateName + "'");
            return null;
        }
        TemplateLookupDTO template = found.get();

        if (isBlank(template.getUrl())) {
            skip(request, templateName, "template '" + templateName + "' has no url");
            return null;
        }
        if (isBlank(template.getReqTemplate())) {
            skip(request, templateName, "template '" + templateName + "' has no req_template");
            return null;
        }

        // Stamped before the render, not after: a template that fails to render is the case where
        // knowing which row was used matters most, and the failure is caught upstream in
        // executeReactive where the name is no longer in scope.
        details.addAdditionalField(field("Template"), templateName);

        ChannelPaymentResponse response = channelResponseMapper.toResponse(request,
                ChannelResponseMapper.scopeFor(context.getDirection(), context.getPipelinePhase()));
        String body = templateRenderer.render(
                template.getReqTemplate(), response, request, extraModel(template, request));

        String contentType = isBlank(template.getContentType())
                ? DEFAULT_CONTENT_TYPE : template.getContentType();

        Map<String, String> headers = headers(template);
        String pipelinePhase = context.getPipelinePhase();
        String notificationType = "SMS";
        if(pipelinePhase.equalsIgnoreCase("email-notification")){
            notificationType = "EMAIL";
        }else{
            notificationType = "SMS";
        }
        applyBearerToken(templateName, headers,notificationType);

        LOG.info("[{}] Dispatching notification: template='{}', url='{}', contentType='{}', requestId='{}'",
                name().toUpperCase(Locale.ROOT), templateName, template.getUrl(), contentType,
                requestId(request));

        return new NotificationDispatch(templateName, template.getUrl(), body, contentType, headers);
    }

    /**
     * A plugin that needs a specific template for one transaction — a different language, a
     * corridor-specific wording — sets {@code additionalFields[overrideKey]}. Everything else falls
     * back to {@code <paymentType>-<SMS|EMAIL>}, so ops enables notifications for a payment type by
     * inserting a row with the right name and nothing else changes.
     *
     * <p>Uses the <em>effective</em> payment type, so a delegated stage
     * ({@code sms-notification>MPESA}) resolves the delegate's template rather than the composite's.
     */
    private String resolveTemplateName(PipelineContext context, Details details) {

/*        String override = details.getAdditionalField(channel().overrideKey());
        if (!isBlank(override)) {
            return override.trim();
        }*/
        String paymentType = context.getEffectivePaymentType();
        if (isBlank(paymentType)) {
            paymentType = details.getPaymentType();
        }
        String pipelinePhase  = context.getPipelinePhase();
        if(pipelinePhase.equalsIgnoreCase("email-notification")){
            return "notification.receiver.EMAIL."+ paymentType + "-"+details.getRequestType();
        }else{
            return "notification.receiver.SMS."+ paymentType + "-"+details.getRequestType();
        }


    }

    /**
     * Static headers for the request. Two sources, both optional:
     * <ul>
     *   <li>{@code additional_fields} entries prefixed {@code header.} — the same convention
     *       {@code Channels.additionalFields} uses for callbacks, and the one to prefer: it is
     *       unbounded and self-describing;</li>
     *   <li>{@code additional_header_1} / {@code additional_header_2}, each written
     *       {@code Name: value} — the two dedicated columns, kept working for rows that already
     *       use them.</li>
     * </ul>
     */
    private Map<String, String> headers(TemplateLookupDTO template) {
        Map<String, String> headers = new LinkedHashMap<>();
        putHeader(headers, template.getAdditionalHeader1());
        putHeader(headers, template.getAdditionalHeader2());
        if (template.getAdditionalFields() != null) {
            template.getAdditionalFields().forEach((key, value) -> {
                if (key != null && key.startsWith(HEADER_PREFIX) && key.length() > HEADER_PREFIX.length()) {
                    headers.put(key.substring(HEADER_PREFIX.length()), value);
                }
            });
        }
        return headers;
    }

    private void putHeader(Map<String, String> headers, String raw) {
        if (isBlank(raw)) {
            return;
        }
        int separator = raw.indexOf(':');
        if (separator <= 0 || separator == raw.length() - 1) {
            LOG.warn("[{}] Ignoring malformed additional header '{}' — expected 'Name: value'", name(), raw);
            return;
        }
        headers.put(raw.substring(0, separator).trim(), raw.substring(separator + 1).trim());
    }

    // -------------------------------------------------------------------------------- bearer auth

    /**
     * Swaps a bearer <em>placeholder</em> in the template's headers for a live OAuth2 token.
     *
     * <p><strong>Opting in is a property of the {@code template_lookup} row.</strong> A row whose
     * {@code additional_header_1} (or {@code additional_header_2}, or a {@code header.Authorization}
     * entry in {@code additional_fields}) is written {@code Authorization: Bearer} declares that this
     * notification provider wants a bearer token. There is no separate flag column and nothing to add
     * to the pipeline config — the same row that says where to POST says how to authenticate.
     *
     * <p>The token itself comes from {@link OAuth2TokenProvider}, the shared refresh-managed registry
     * the plugins and {@code callback} already use, under the profile id
     * {@code notification.<templateName>}. Credentials are read from {@code app_configs} keyed by the
     * template name (see {@link #readConfig}), and the profile is (re-)registered on every
     * notification: {@link OAuth2TokenProvider#registerProfile} no-ops on identical credentials, so
     * this costs nothing in steady state and gives free rotation — updating an {@code app_configs}
     * row makes the next notification cancel the old refresh task and fetch against the new
     * credentials, with no Core restart.
     *
     * <p><strong>The header is only ever replaced on success.</strong> If the credentials are
     * incomplete or the token endpoint fails, the header goes out exactly as the row configured it.
     * That keeps the legacy case working — a row carrying a real static token
     * ({@code Authorization: Bearer eyJhbGci...}) and no {@code app_configs} credentials still sends
     * that token — and means a token outage degrades to the provider's own 401, recorded as
     * {@code FAILED} on the transaction, rather than a silently unauthenticated notification.
     */
    private void applyBearerToken(String templateName, Map<String, String> headers,String smsOrEmail) {
        String headerKey = null;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (AUTHORIZATION.equalsIgnoreCase(entry.getKey()) && startsWithBearer(entry.getValue())) {
                headerKey = entry.getKey();
                break;
            }
        }
        if (headerKey == null) {
            return;
        }

        String token = fetchBearerTokenOrNull(smsOrEmail);
        if (token == null) {
            return;
        }
        headers.put(headerKey, BEARER + " " + token);
        LOG.debug("[{}] Applied OAuth2 bearer token to template '{}'", name(), templateName);
    }

    private static boolean startsWithBearer(String value) {
        if (isBlank(value)) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.regionMatches(true, 0, BEARER, 0, BEARER.length());
    }

    /**
     * @return a bearer token for {@code templateName}, or {@code null} when the template has no
     *         (complete) OAuth2 credentials in {@code app_configs} or the token endpoint refused —
     *         both of which leave the configured header untouched
     */
    private String fetchBearerTokenOrNull(String notificationBase) {
        Optional<TokenProfile> profile = buildProfile(notificationBase);
        if (profile.isEmpty()) {
            return null;
        }
        try {
            // Both calls block: registerProfile fetches synchronously the first time, so bad
            // credentials surface loudly, and getToken is an in-memory read afterwards. Legal here
            // because resolve() already runs on boundedElastic.
            tokenProvider.registerProfile(profile.get());
            return tokenProvider.getToken(profile.get().profileId());
        } catch (RuntimeException ex) {
            LOG.error("[{}] Failed to obtain an OAuth2 token for template '{}' (profile '{}'): {}. "
                            + "Sending the notification with the Authorization header as configured.",
                    name().toUpperCase(Locale.ROOT), notificationBase, profile.get().profileId(),
                    ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Builds the refresh profile from {@code app_configs}. Required: {@code token-url},
     * {@code client-id}, {@code client-secret}. Optional: {@code scope}, {@code auth-method}
     * ({@code BASIC} default, {@code POST} alt) and {@code token-request-method} ({@code POST}
     * form-encoded default, {@code POST_JSON} / {@code GET} alts) — the same vocabulary
     * {@code callback.<channelName>.*} uses, so ops learns one scheme.
     */
    private Optional<TokenProfile> buildProfile(String templateName) {
        String tokenUrl = readConfig(templateName, "token-url");
        String clientId = readConfig(templateName, "client-id", "api-key", "api-key-id");
        String clientSecret = readConfig(templateName, "client-secret", "api-secret");

        if (tokenUrl == null || clientId == null || clientSecret == null) {
            if (incompleteCredsWarned.add(templateName)) {
                LOG.warn("[{}] Template '{}' asks for bearer auth but its OAuth2 config in app_configs "
                                + "is incomplete (needs '{}{}.token-url', '{}{}.client-id', "
                                + "'{}{}.client-secret'). Sending the Authorization header as configured.",
                        name().toUpperCase(Locale.ROOT), templateName,
                        CONFIG_PREFIX, templateName, CONFIG_PREFIX, templateName,
                        CONFIG_PREFIX, templateName);
            }
            return Optional.empty();
        }
        // Complete now — clear the latch so a future regression warns again.
        incompleteCredsWarned.remove(templateName);

        ClientAuthMethod authMethod = "POST".equalsIgnoreCase(readConfig(templateName, "auth-method"))
                ? ClientAuthMethod.POST : ClientAuthMethod.BASIC;

        String requestMethodName = readConfig(templateName, "token-request-method");
        TokenRequestMethod requestMethod;
        if ("POST_JSON".equalsIgnoreCase(requestMethodName)) {
            requestMethod = TokenRequestMethod.POST_JSON;
        } else if ("GET".equalsIgnoreCase(requestMethodName)) {
            requestMethod = TokenRequestMethod.GET;
        } else {
            requestMethod = TokenRequestMethod.POST;
        }

        return Optional.of(new TokenProfile(
                CONFIG_PREFIX + templateName,
                tokenUrl, clientId, clientSecret,
                readConfig(templateName, "scope"),
                GrantType.CLIENT_CREDENTIALS,
                authMethod,
                0.8,
                Duration.ofSeconds(30),
                Duration.ofSeconds(30),
                requestMethod));
    }

    /**
     * Reads one credential for a template, trying each alias in turn and, for each, both the
     * namespaced {@code notification.<templateName>.<key>} form and the bare
     * {@code <templateName>.<key>} form. Prefer the namespaced one: {@code app_configs} is a flat key
     * space shared with every plugin's {@code <paymentType>.*} rows, and the prefix is what keeps a
     * template's credentials recognisably a Core notification setting.
     *
     * <p>All reads hit the {@code appConfigs} cache, so the alias sweep is effectively free.
     */
    private String readConfig(String templateName, String... keys) {
        for (String key : keys) {
            String value = readConfigId(CONFIG_PREFIX + templateName + "." + key);
            if (value == null) {
                value = readConfigId(templateName + "." + key);
            }
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String readConfigId(String configId) {
        return appConfigsLookup.findByConfigId(configId)
                .map(AppConfigsSPI::getConfigValue)
                .filter(value -> !value.isBlank())
                .orElse(null);
    }

    /**
     * Template-owned variables on top of the shared callback model, so a row can carry its own
     * constants — an SMS sender id in {@code param_1}, a mail "from" address in {@code param_2} —
     * without a config lookup in the template.
     *
     * <p>Also supplies <strong>{@code ${formattedAmount}}</strong>, and templates should use it in
     * preference to {@code ${response.amount}}: the amount reaches Freemarker as a
     * {@link BigDecimal}, which Freemarker renders through the locale's number format, so
     * {@code 5000.00} comes out as {@code 5,000} — grouped, and with the minor units gone. That is
     * merely ugly in an SMS body but produces invalid JSON in a payload, and it silently drops
     * precision either way. {@code formattedAmount} is {@code toPlainString()}: no grouping, scale
     * preserved.
     *
     * <p>The number format cannot simply be fixed in {@link CallbackTemplateRenderer} — it is shared
     * with {@code callback}, whose live templates render against the current behaviour, and changing
     * it would silently reshape every callback payload in production.
     */
    private Map<String, Object> extraModel(TemplateLookupDTO template, PaymentLogSPI request) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("param1", template.getParam1());
        extra.put("param2", template.getParam2());
        extra.put("providerCode", template.getProviderCode());
        extra.put("templateName", template.getTemplateName());
        if (template.getAdditionalFields() != null) {
            extra.put("template", template.getAdditionalFields());
        }
        BigDecimal amount = request.getDetails() == null ? null : request.getDetails().getAmount();
        extra.put("formattedAmount", amount == null ? "" : amount.toPlainString());
        return extra;
    }

    // --------------------------------------------------------------------------------- dispatch

    private Mono<CallbackHttpClient.ClientHttpResponse> send(NotificationDispatch dispatch, PaymentLogSPI request) {
        String contentType = dispatch.contentType().toLowerCase(Locale.ROOT).trim();

        Mono<CallbackHttpClient.ClientHttpResponse> call;
        if (contentType.contains("application/json")) {
            call = httpClient.postJson(dispatch.url(), dispatch.body(), dispatch.headers(), null);
        } else if (contentType.contains("soap")) {
            call = httpClient.postSoap(dispatch.url(), dispatch.body(), dispatch.headers(), null);
        } else if (contentType.contains("application/xml") || contentType.contains("text/xml")) {
            call = httpClient.postXml(dispatch.url(), dispatch.body(), dispatch.headers(), null);
        } else {
            LOG.warn("[{}] Unknown content type '{}' on template '{}', defaulting to JSON",
                    name(), dispatch.contentType(), dispatch.templateName());
            call = httpClient.postJson(dispatch.url(), dispatch.body(), dispatch.headers(), null);
        }

        return call.doOnNext(response -> {
            // CallbackHttpClient never signals an error — a transport failure arrives as a
            // synthetic negative status code carrying the exception message as the body.
            boolean delivered = response.statusCode() >= 200 && response.statusCode() < 300;
            LOG.info("[{}] Response: statusCode={}, template='{}', requestId='{}'",
                    name().toUpperCase(Locale.ROOT), response.statusCode(),
                    dispatch.templateName(), requestId(request));
            record(request, delivered ? "SENT" : "FAILED", response.statusCode(), response.body());
        });
    }

    // ---------------------------------------------------------------------------------- outcome

    private void skip(PaymentLogSPI request, String templateName, String reason) {
        LOG.warn("[{}] Skipping notification for requestId='{}': {}",
                name().toUpperCase(Locale.ROOT), requestId(request), reason);
        if (request.getDetails() != null) {
            request.getDetails().addAdditionalField(field("Template"), templateName);
        }
        record(request, "SKIPPED", null, reason);
    }

    /**
     * Write the outcome onto {@code Details.additionalFields} under this channel's prefix, e.g.
     * {@code smsNotificationStatus} / {@code smsNotificationHttpStatus} /
     * {@code smsNotificationResponse}. Whatever happened, the transaction record says so.
     */
    private void record(PaymentLogSPI request, String status, Integer httpStatus, String response) {
        Details details = request.getDetails();
        if (details == null) {
            return;
        }
        details.addAdditionalField(field("Status"), status);
        if (httpStatus != null) {
            details.addAdditionalField(field("HttpStatus"), String.valueOf(httpStatus));
        }
        if (!isBlank(response)) {
            details.addAdditionalField(field("Response"), truncate(response, MAX_RECORDED_RESPONSE));
        }
    }

    private String field(String suffix) {
        return channel().fieldPrefix() + "Notification" + suffix;
    }

    // ----------------------------------------------------------------------------------- helpers

    private static String requestId(PaymentLogSPI request) {
        return request.getDetails() != null ? request.getDetails().getRequestId() : null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }

    /** Everything {@link #resolve} worked out, ready to put on the wire. */
    private record NotificationDispatch(String templateName, String url, String body,
                                        String contentType, Map<String, String> headers) {
    }
}
