package com.dotechly.gotechly.core.config.drain;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Counts requests in flight on this instance so {@link NodeDrainService} can report when a drained
 * node has genuinely gone quiet.
 *
 * <p><b>The async trap this exists to avoid.</b> Core is {@code spring-boot-starter-web} (Tomcat)
 * with controllers that return {@code Mono} — WebFlux is on the classpath for {@code WebClient},
 * not as the server. So {@code filterChain.doFilter} returns as soon as the controller hands back
 * its publisher, long before the payment finishes. Decrementing there would drive the counter to
 * zero while postings are still running and tell the operator it is safe to swap a plugin JAR
 * mid-transaction — the exact failure this whole mechanism is meant to prevent. The decrement
 * therefore happens in {@link AsyncListener#onComplete}.
 *
 * <p>{@code OncePerRequestFilter} does not re-run on ASYNC or ERROR dispatches by default, which is
 * what we want here: the request is counted once, on its initial dispatch. (Contrast
 * {@code InternalApiKeyFilter}, which must opt <em>in</em> to those dispatches to keep the security
 * context populated.)
 *
 * <p>Excluded from counting: the actuator, this node's own drain endpoints — a status poll that
 * counted itself could never observe zero — and the jPOS metrics read the Gateway polls on every
 * instance directly.
 */
public class InFlightRequestFilter extends OncePerRequestFilter {

    private final NodeDrainService drainService;

    public InFlightRequestFilter(NodeDrainService drainService) {
        this.drainService = drainService;
    }

    /** Polled per instance (not via the LB) every 10s while the portal's jPOS dashboard is open. */
    static final String JPOS_METRICS_PATH = "/api/v1/admin/jpos/metrics";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/")
                || path.startsWith("/api/v1/admin/node/")
                // A read-only monitoring poll the Gateway sends straight to each instance, drained or
                // not. Counting it made a quiet node flicker back to "1 request in flight" every 10s
                // whenever someone had the jPOS page open. Only the GET: the jPOS *scale* commands
                // change running state and must still hold a drain open.
                || ("GET".equals(request.getMethod()) && path.equals(JPOS_METRICS_PATH));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final long token = drainService.beginRequest(
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                request.getHeader("X-Forwarded-For"));
        // One-shot: the container may deliver onError/onTimeout *and then* onComplete for the same
        // request. A counter must not decrement twice.
        final AtomicBoolean settled = new AtomicBoolean();

        try {
            filterChain.doFilter(request, response);
        } catch (RuntimeException | ServletException | IOException ex) {
            release(settled, token);
            throw ex;
        }

        if (request.isAsyncStarted()) {
            try {
                request.getAsyncContext().addListener(new CountingListener(settled, token));
                return;
            } catch (IllegalStateException alreadyFinished) {
                // Async completed between doFilter() returning and this registration.
                release(settled, token);
                return;
            }
        }
        release(settled, token);
    }

    private void release(AtomicBoolean settled, long token) {
        if (settled.compareAndSet(false, true)) {
            drainService.endRequest(token);
        }
    }

    private final class CountingListener implements AsyncListener {

        private final AtomicBoolean settled;
        private final long token;

        private CountingListener(AtomicBoolean settled, long token) {
            this.settled = settled;
            this.token = token;
        }

        @Override
        public void onComplete(AsyncEvent event) {
            release(settled, token);
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            release(settled, token);
        }

        @Override
        public void onError(AsyncEvent event) {
            release(settled, token);
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            // Listeners are NOT carried across a re-dispatch that starts async again; the servlet
            // spec requires re-registration here. Without it the request would be released on the
            // first cycle's completion while it is still running.
            event.getAsyncContext().addListener(this);
        }
    }
}
