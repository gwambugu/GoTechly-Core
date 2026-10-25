package com.dotechly.gotechly.core.service.auth;


import com.dotechly.gotechly.spi.service.auth.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OAuth2TokenProviderImpl implements OAuth2TokenProvider, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(OAuth2TokenProviderImpl.class);

    /**
     * Parses the token endpoint's response body ourselves rather than letting WebClient
     * pick a decoder from the response {@code Content-Type}. Some providers (notably
     * Safaricom Daraja) return a JSON token body but mislabel it as
     * {@code application/x-www-form-urlencoded}, which would otherwise fail content
     * negotiation with {@code UnsupportedMediaTypeException}.
     */
    private static final ObjectMapper TOKEN_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final WebClient httpClient;
    private final ScheduledExecutorService scheduler;
    private final OAuth2TokenProviderProperties props;
    private final ConcurrentMap<String, ProfileState> profiles = new ConcurrentHashMap<>();

    public OAuth2TokenProviderImpl(
            @Qualifier("oauth2TokenWebClient") WebClient httpClient,
            @Qualifier("oauth2RefreshScheduler") ScheduledExecutorService scheduler,
            OAuth2TokenProviderProperties props) {
        this.httpClient = httpClient;
        this.scheduler = scheduler;
        this.props = props;
    }

    // =========================================================================
    // SPI
    // =========================================================================

    @Override
    public void registerProfile(TokenProfile profile) {
        ProfileState existing = profiles.get(profile.profileId());
        if (existing != null && existing.profile.credentialsEqual(profile)) {
            LOG.debug("Token profile '{}' already registered with identical credentials — no-op",
                    profile.profileId());
            return;
        }

        ProfileState next = new ProfileState(profile);
        ProfileState prev = profiles.put(profile.profileId(), next);
        if (prev != null) {
            LOG.info("Replacing token profile '{}' (credentials changed)", profile.profileId());
            cancelRefresh(prev);
        } else {
            LOG.info("Registering token profile '{}'", profile.profileId());
        }

        // Synchronous initial fetch — if creds are bad, plugin init fails loud.
        CachedToken fresh = fetchSingleFlight(next).join();
        scheduleRefresh(next, fresh);
    }

    @Override
    public String getToken(String profileId) {
        ProfileState st = profiles.get(profileId);
        if (st == null) {
            throw new TokenUnavailableException("Unknown token profile: " + profileId);
        }
        CachedToken t = st.token;
        if (t != null && Instant.now().isBefore(t.expiresAt())) {
            return t.value();
        }
        // No valid cached token — try a synchronous single-flight fetch.
        try {
            return fetchSingleFlight(st).join().value();
        } catch (CompletionException ce) {
            Throwable root = ce.getCause() != null ? ce.getCause() : ce;
            throw new TokenUnavailableException(
                    "No valid token for profile=" + profileId + " and on-demand fetch failed: " + root.getMessage(),
                    root);
        }
    }

    @Override
    public void unregisterProfile(String profileId) {
        ProfileState st = profiles.remove(profileId);
        if (st != null) {
            cancelRefresh(st);
            LOG.info("Unregistered token profile '{}'", profileId);
        }
    }

    @Override
    public boolean hasProfile(String profileId) {
        return profiles.containsKey(profileId);
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    public void destroy() {
        profiles.values().forEach(this::cancelRefresh);
        profiles.clear();
        // Scheduler shutdown is handled by the bean's destroyMethod = "shutdownNow"
    }

    // =========================================================================
    // Internals
    // =========================================================================

    /**
     * At most one fetch per profile is in flight at any time. Concurrent callers
     * share the same future via CAS on {@code inFlight}.
     */
    private CompletableFuture<CachedToken> fetchSingleFlight(ProfileState st) {
        CompletableFuture<CachedToken> existing = st.inFlight.get();
        if (existing != null) return existing;

        CompletableFuture<CachedToken> mine = new CompletableFuture<>();
        if (!st.inFlight.compareAndSet(null, mine)) {
            // Another thread won the race; share its future.
            return st.inFlight.get();
        }
        try {
            CachedToken fresh = doHttpFetch(st.profile);
            st.token = fresh;
            mine.complete(fresh);
            return mine;
        } catch (TokenFetchException e) {
            mine.completeExceptionally(e);
            throw e;
        } catch (RuntimeException e) {
            // Anything else is treated as retryable transport failure.
            TokenFetchException wrapped = new TokenFetchException(
                    "Unexpected fetch failure for profile=" + st.profile.profileId(), e, true);
            mine.completeExceptionally(wrapped);
            throw wrapped;
        } finally {
            st.inFlight.set(null);
        }
    }

    private void scheduleRefresh(ProfileState st, CachedToken fresh) {
        long ttlMillis = Duration.between(Instant.now(), fresh.expiresAt()).toMillis();
        long lead = Math.max(
                st.profile.minRefreshInterval().toMillis(),
                (long) (ttlMillis * st.profile.refreshLeadFraction()));
        synchronized (st) {
            cancelRefresh(st);
            st.refreshTask = scheduler.schedule(
                    () -> refreshWithBackoff(st), lead, TimeUnit.MILLISECONDS);
        }
        LOG.debug("Scheduled refresh for profile '{}' in {}ms (effective TTL {}ms)",
                st.profile.profileId(), lead, ttlMillis);
    }

    /**
     * Background refresh: keeps serving the still-valid cached token if the
     * fetch fails transiently; falls back to a long delay for non-retryable
     * (auth-rejection) failures so we do not hammer the provider's auth endpoint.
     */
    private void refreshWithBackoff(ProfileState st) {
        // If the profile has been unregistered while we were queued, exit.
        if (!profiles.containsKey(st.profile.profileId())) return;
        try {
            CachedToken fresh = fetchSingleFlight(st).join();
            scheduleRefresh(st, fresh);
        } catch (CompletionException ce) {
            Throwable root = ce.getCause() != null ? ce.getCause() : ce;
            boolean retryable = !(root instanceof TokenFetchException tfe) || tfe.isRetryable();
            Duration next = retryable ? props.backoff().retryableDelay() : props.backoff().nonRetryableDelay();
            LOG.warn("Token refresh failed for '{}' (retryable={}). Next attempt in {}.",
                    st.profile.profileId(), retryable, next, root);
            synchronized (st) {
                st.refreshTask = scheduler.schedule(
                        () -> refreshWithBackoff(st), next.toMillis(), TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            // Defensive: never let a refresh-loop exception kill the scheduler thread.
            LOG.error("Unexpected error in refresh loop for '{}'", st.profile.profileId(), e);
            Duration next = props.backoff().retryableDelay();
            synchronized (st) {
                st.refreshTask = scheduler.schedule(
                        () -> refreshWithBackoff(st), next.toMillis(), TimeUnit.MILLISECONDS);
            }
        }
    }

    private void cancelRefresh(ProfileState st) {
        synchronized (st) {
            if (st.refreshTask != null) {
                st.refreshTask.cancel(false);
                st.refreshTask = null;
            }
        }
    }

    /**
     * Build the token-endpoint request for a profile.
     * <ul>
     *   <li>{@link TokenRequestMethod#POST} (default, RFC 6749): form-encoded body carrying
     *       {@code grant_type} (+ {@code scope}, + {@code client_id}/{@code client_secret}
     *       when {@link ClientAuthMethod#POST}); Basic header when {@link ClientAuthMethod#BASIC}.</li>
     *   <li>{@link TokenRequestMethod#GET} (Daraja): {@code grant_type} (+ {@code scope}) as
     *       query params, credentials always in the {@code Authorization: Basic} header, no body.
     *       A POST to Daraja's {@code /oauth/v1/generate} is not handled as a token request —
     *       it echoes the request body back instead of returning JSON.</li>
     * </ul>
     */
    private WebClient.RequestHeadersSpec<?> buildTokenRequest(TokenProfile p) {
        LOG.info("Token fetch for profile='{}': method={} authMethod={} url={}",
                p.profileId(), p.requestMethod(), p.authMethod(), p.tokenUrl());
        if (p.requestMethod() == TokenRequestMethod.GET) {
            UriComponentsBuilder b = UriComponentsBuilder.fromUriString(p.tokenUrl())
                    .replaceQueryParam("grant_type", p.grantType().wireValue());
            if (p.scope() != null && !p.scope().isBlank()) {
                b.replaceQueryParam("scope", p.scope());
            }
            URI uri = b.encode().build().toUri();
            return httpClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(h -> h.setBasicAuth(p.clientId(), p.clientSecret(), StandardCharsets.UTF_8));
        }

        if (p.requestMethod() == TokenRequestMethod.POST_JSON) {
            Map<String, Object> jsonBody = new LinkedHashMap<>();
            jsonBody.put("username", p.clientId());
            jsonBody.put("password", p.clientSecret());
            if (p.scope() != null && !p.scope().isBlank()) {
                jsonBody.put("scope", p.scope());
            }
            LOG.info("[TOKEN] >> POST {} | body: {}", p.tokenUrl(), jsonBody);
            return httpClient.post()
                    .uri(p.tokenUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonBody);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", p.grantType().wireValue());
        if (p.scope() != null && !p.scope().isBlank()) {
            form.add("scope", p.scope());
        }
        if (p.authMethod() == ClientAuthMethod.POST) {
            form.add("client_id", p.clientId());
            form.add("client_secret", p.clientSecret());
        }

        WebClient.RequestBodySpec req = httpClient.post()
                .uri(p.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON);

        if (p.authMethod() == ClientAuthMethod.BASIC) {
            req.headers(h -> h.setBasicAuth(p.clientId(), p.clientSecret(), StandardCharsets.UTF_8));
        }
        return req.body(BodyInserters.fromFormData(form));
    }

    private CachedToken doHttpFetch(TokenProfile p) {
        WebClient.RequestHeadersSpec<?> spec = buildTokenRequest(p);

        // Fetch the raw body as a String — the StringDecoder accepts any media type
        // (`*/*`), so a provider that mislabels its JSON token body (e.g. Daraja's
        // `application/x-www-form-urlencoded`) still decodes, and we parse it ourselves.
        String rawBody;
        try {
            rawBody = spec
                    .retrieve()
                    .onStatus(s -> s.is4xxClientError(), resp -> resp.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(b -> new TokenFetchException(
                                    "Auth rejected by provider for profile=" + p.profileId()
                                            + " status=" + resp.statusCode().value()
                                            + " body=" + truncate(b, 500),
                                    false)))
                    .onStatus(s -> s.is5xxServerError(), resp -> resp.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(b -> new TokenFetchException(
                                    "Provider 5xx for profile=" + p.profileId()
                                            + " status=" + resp.statusCode().value()
                                            + " body=" + truncate(b, 500),
                                    true)))
                    .bodyToMono(String.class)
                    .timeout(props.responseTimeout())
                    .onErrorMap(WebClientRequestException.class,
                            e -> new TokenFetchException(
                                    "Transport failure for profile=" + p.profileId() + ": " + e.getMessage(),
                                    e, true))
                    .block();
        } catch (TokenFetchException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenFetchException(
                    "Token fetch failed for profile=" + p.profileId() + ": " + e.getMessage(), e, true);
        }

        LOG.info("[TOKEN] << POST {} | body: {}", p.tokenUrl(), rawBody);
        TokenResponse body = parseTokenResponse(rawBody, p.profileId());

        // Resolve the bearer value — standard providers use "access_token"; DFS Router uses "token"
        String resolvedToken = body.accessToken() != null ? body.accessToken() : body.token();
        if (body == null || resolvedToken == null || resolvedToken.isBlank()) {
            throw new TokenFetchException(
                    "Empty access_token in response for profile=" + p.profileId(), false);
        }
        if (body.tokenType() != null && !"bearer".equalsIgnoreCase(body.tokenType())) {
            LOG.warn("Profile '{}' returned non-bearer token_type='{}' — treating as bearer.",
                    p.profileId(), body.tokenType());
        }

        long ttlSeconds = body.expiresIn() != null ? body.expiresIn() : 3600L;
        Instant expiresAt = Instant.now()
                .plusSeconds(ttlSeconds)
                .minus(p.clockSkewBuffer());
        // Guard against pathological provider responses that would expire the token
        // before we even cache it (e.g. expires_in <= clockSkewBuffer).
        if (Instant.now().isAfter(expiresAt)) {
            throw new TokenFetchException(
                    "Token for profile=" + p.profileId()
                            + " would expire immediately (expires_in=" + ttlSeconds
                            + "s, clockSkewBuffer=" + p.clockSkewBuffer() + ")",
                    false);
        }

        LOG.info("Fetched token profile='{}' ttl={}s effective-expiry={}",
                p.profileId(), ttlSeconds, expiresAt);
        return new CachedToken(resolvedToken, expiresAt);
    }

    /**
     * Deserialize the raw token-endpoint body into {@link TokenResponse} regardless of
     * the response Content-Type. A blank body or unparseable payload is a contract
     * mismatch (not a transient transport fault), so it is non-retryable — retrying
     * would only spam the auth endpoint with the same result.
     */
    private TokenResponse parseTokenResponse(String raw, String profileId) {
        if (raw == null || raw.isBlank()) {
            throw new TokenFetchException(
                    "Empty token response body for profile=" + profileId, false);
        }
        try {
            return TOKEN_MAPPER.readValue(raw, TokenResponse.class);
        } catch (Exception e) {
            throw new TokenFetchException(
                    "Unparseable token response for profile=" + profileId
                            + ": " + truncate(raw, 500),
                    e, false);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    // =========================================================================
    // Internal types
    // =========================================================================

    private static final class ProfileState {
        final TokenProfile profile;
        volatile CachedToken token;
        final AtomicReference<CompletableFuture<CachedToken>> inFlight = new AtomicReference<>();
        ScheduledFuture<?> refreshTask;

        ProfileState(TokenProfile profile) {
            this.profile = profile;
        }
    }

    private record CachedToken(String value, Instant expiresAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token") String token,          // DFS Router returns "token" instead of "access_token"
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("scope") String scope) {

        @SuppressWarnings("unused")
        public String tokenTypeNormalized() {
            return tokenType == null ? null : tokenType.toLowerCase(Locale.ROOT);
        }
    }
}
