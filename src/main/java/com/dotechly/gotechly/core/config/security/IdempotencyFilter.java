package com.dotechly.gotechly.core.config.security;


import com.dotechly.gotechly.core.service.idempotency.IdempotencyRecord;
import com.dotechly.gotechly.core.service.idempotency.IdempotencyService;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Cluster-wide idempotency / dedup for POST-safe retries, keyed on a client-supplied
 * {@code Idempotency-Key} header and backed by {@link IdempotencyService} (Hazelcast).
 * <p>
 * Opt-in: only POSTs that carry the header and target a configured path prefix are processed; all
 * other traffic passes straight through untouched. The first request to claim a key processes
 * normally and the key is marked DONE on completion. A duplicate that arrives while the original is
 * still in flight — or after it completed — receives <b>HTTP 409 Conflict</b> and should query
 * transaction status rather than re-submit.
 * <p>
 * <b>Why 409 rather than replaying the original response body:</b> Core's payment endpoints return
 * reactive {@code Mono}s served over async dispatch, where buffering and replaying the exact
 * response body via servlet wrappers is unreliable (Spring's own {@code ShallowEtagHeaderFilter}
 * disables itself for async requests for the same reason). The at-most-once + conflict model
 * guarantees the request is never executed twice — the property that actually matters for payments
 * — without depending on fragile async body capture. If transparent replay is later required,
 * capture the response in the owning request's {@link AsyncListener#onComplete} and store it on the
 * {@link IdempotencyRecord}.
 */
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(IdempotencyFilter.class);

    private final IdempotencyService idempotencyService;
    private final IdempotencyProperties properties;

    public IdempotencyFilter(IdempotencyService idempotencyService, IdempotencyProperties properties) {
        this.idempotencyService = idempotencyService;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled() || !"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String key = request.getHeader(properties.getHeaderName());
        if (key == null || key.isBlank()) {
            return true;   // opt-in: no key → no dedup
        }
        String path = request.getRequestURI();
        return properties.getPathPrefixes().stream().noneMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String key = request.getHeader(properties.getHeaderName());

        var prior = idempotencyService.begin(key);
        if (prior.isPresent()) {
            writeConflict(response, key, prior.get());
            return;
        }

        // We own first processing of this key. Release it on hard failure so a genuine retry can
        // re-process; mark DONE once a response status is known (sync inline, or async on complete).
        try {
            filterChain.doFilter(request, response);
        } catch (RuntimeException | ServletException | IOException ex) {
            idempotencyService.release(key);
            throw ex;
        }

        if (request.isAsyncStarted()) {
            request.getAsyncContext().addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) {
                    settle(key, response);
                }
                @Override
                public void onTimeout(AsyncEvent event) {
                    idempotencyService.release(key);
                }
                @Override
                public void onError(AsyncEvent event) {
                    idempotencyService.release(key);
                }
                @Override
                public void onStartAsync(AsyncEvent event) {
                    // no-op
                }
            });
        } else {
            settle(key, response);
        }
    }

    /** Mark DONE for a successful (2xx) response; otherwise release the key for retry. */
    private void settle(String key, HttpServletResponse response) {
        try {
            int status = response.getStatus();
            if (status >= 200 && status < 300) {
                idempotencyService.complete(key, status, response.getContentType(), null);
            } else {
                idempotencyService.release(key);
            }
        } catch (Exception e) {
            LOG.warn("Idempotency settle failed for key '{}': {}", key, e.getMessage());
            idempotencyService.release(key);
        }
    }

    private void writeConflict(HttpServletResponse response, String key, IdempotencyRecord prior)
            throws IOException {
        String message = prior.isInFlight()
                ? "A request with this Idempotency-Key is still being processed."
                : "A request with this Idempotency-Key was already processed. Query transaction status instead of retrying.";
        LOG.info("Duplicate request rejected: idempotencyKey='{}', priorStatus={}", key, prior.status());
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"DUPLICATE_REQUEST\",\"idempotencyKey\":\"" + escape(key) + "\","
                        + "\"priorStatus\":\"" + escape(prior.status()) + "\","
                        + "\"message\":\"" + escape(message) + "\"}");
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
