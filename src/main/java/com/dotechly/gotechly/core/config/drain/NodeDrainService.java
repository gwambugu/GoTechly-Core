package com.dotechly.gotechly.core.config.drain;

import com.dotechly.gotechly.core.config.InstanceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds this instance's {@link DrainState} and counts the requests still running on it, so an
 * operator can take one Core out of the Gateway's routing pool and know — rather than guess — when
 * it is safe to deploy a plugin and restart.
 *
 * <p><b>Why the node owns this.</b> The Gateway already filters instances on a health probe
 * ({@code GoTechlyCoreLbConfig} → {@code withHealthChecks}), so a node that can answer "do not
 * route to me" is removed with no routing code at all. And only the node can answer the question
 * that actually gates the deploy: is anything still running here?
 *
 * <p><b>Drain is not instant.</b> Flipping the flag stops <em>new</em> work only after the LB's next
 * probe (interval 10s) plus its instance-list cache. Work already accepted keeps running — a CBS
 * posting may sit for up to 30s. Gate on {@link Status#quiet()}, never on elapsed time.
 *
 * <p>See {@code docs/controlled-plugin-deployment.md}.
 */
@Service
public class NodeDrainService {

    private static final Logger LOG = LoggerFactory.getLogger(NodeDrainService.class);

    private final InstanceInfo instanceInfo;

    private final AtomicReference<DrainState> state = new AtomicReference<>(DrainState.ROUTABLE);
    private volatile Instant stateSince = Instant.now();

    /**
     * Sequence → start-millis for every request currently in flight.
     *
     * <p>A skip-list map rather than a plain counter because the operator needs the age of the
     * <em>oldest</em> survivor: a request older than the widest CBS timeout is stuck, not slow, and
     * that is worth knowing before restarting over it. Sequences are monotonic, so the lowest key
     * still present is the oldest request and {@code firstEntry()} is O(log n).
     */
    private final ConcurrentSkipListMap<Long, InFlightRequest> inFlight = new ConcurrentSkipListMap<>();

    /**
     * How many in-flight requests {@link #status()} itemises. The count stays exact; only the list
     * is capped, so a node that is (wrongly) still taking full traffic cannot balloon the response.
     */
    static final int MAX_LISTED_REQUESTS = 20;

    /**
     * Requests that <em>arrived</em> while this node was draining, newest first, capped at
     * {@link #MAX_LISTED_REQUESTS}.
     *
     * <p>This is the diagnostic the in-flight list alone cannot give. A drained node should receive
     * nothing new, so every entry here is traffic that bypassed the load balancer's {@code /routable}
     * probe — a Gateway still probing {@code /actuator/health}, a caller hitting Core's port
     * directly, or a plugin calling back to {@code core.base-url} (localhost). Most such requests
     * finish in milliseconds and are gone before a 3-second dashboard poll can see them in flight;
     * the remote address recorded here says which of those it was. Reset on every drain.
     */
    private final ConcurrentLinkedDeque<RequestView> recentArrivals = new ConcurrentLinkedDeque<>();
    private final AtomicLong arrivalsWhileDraining = new AtomicLong();
    private volatile Instant lastArrivalWarnAt;
    private static final Duration ARRIVAL_WARN_INTERVAL = Duration.ofSeconds(30);
    private final AtomicLong sequence = new AtomicLong();

    /**
     * Kept alongside the map purely so the count is O(1). Mutated only on a confirmed map
     * insertion/removal, so it cannot drift from {@link #inFlight}.
     */
    private final AtomicInteger inFlightCount = new AtomicInteger();

    /** Who asked for the current state. Incident data: a forgotten drain has an owner. */
    private volatile String requestedBy = "system";
    private volatile Instant lastWarnAt;

    private final Duration warnAfter;
    private final Duration warnInterval;

    public NodeDrainService(InstanceInfo instanceInfo,
                            @Value("${gotechly.drain.warn-after:PT15M}") Duration warnAfter,
                            @Value("${gotechly.drain.warn-interval:PT15M}") Duration warnInterval) {
        this.instanceInfo = instanceInfo;
        this.warnAfter = warnAfter;
        this.warnInterval = warnInterval;
    }

    // =========================================================================
    // State
    // =========================================================================

    /**
     * @param requestedBy the portal user who asked, forwarded by the Gateway. Recorded because a
     *                    drain is a traffic kill switch: when a node is found still drained hours
     *                    later, the first question is who took it out and whether they are done.
     * @return {@code true} if this call changed the state, {@code false} if already draining.
     */
    public boolean drain(String requestedBy) {
        if (state.compareAndSet(DrainState.ROUTABLE, DrainState.DRAINING)) {
            this.requestedBy = normalise(requestedBy);
            stateSince = Instant.now();
            lastWarnAt = null;
            recentArrivals.clear();
            arrivalsWhileDraining.set(0);
            lastArrivalWarnAt = null;
            LOG.warn("NODE_DRAIN instance={} state=DRAINING requestedBy={} inFlight={} — this instance "
                            + "will be removed from the Gateway load-balancer pool at its next health "
                            + "probe. Requests already accepted continue to completion.",
                    instanceInfo.getInstanceId(), this.requestedBy, inFlightCount.get());
            return true;
        }
        LOG.info("NODE_DRAIN instance={} already DRAINING since {} (requestedBy={})",
                instanceInfo.getInstanceId(), stateSince, this.requestedBy);
        return false;
    }

    /** @return {@code true} if this call changed the state, {@code false} if already routable. */
    public boolean undrain(String requestedBy) {
        if (state.compareAndSet(DrainState.DRAINING, DrainState.ROUTABLE)) {
            String drainedBy = this.requestedBy;
            this.requestedBy = normalise(requestedBy);
            stateSince = Instant.now();
            lastWarnAt = null;
            LOG.warn("NODE_DRAIN instance={} state=ROUTABLE requestedBy={} (was drained by {}) — this "
                            + "instance will re-enter the load-balancer pool at its next health probe.",
                    instanceInfo.getInstanceId(), this.requestedBy, drainedBy);
            return true;
        }
        LOG.info("NODE_DRAIN instance={} already ROUTABLE", instanceInfo.getInstanceId());
        return false;
    }

    private static String normalise(String requestedBy) {
        return requestedBy == null || requestedBy.isBlank() ? "unknown" : requestedBy.trim();
    }

    /**
     * Warns, repeatedly, while this instance has been drained for longer than a deploy should take.
     *
     * <p>A forgotten drain is silent by construction: the platform keeps working on its remaining
     * instances, latency looks normal, and nothing fails. What has actually happened is that the
     * cluster is running with reduced capacity that nobody has accounted for — until peak load
     * arrives, or another instance dies and the fallback that was supposed to exist is already
     * spent.
     *
     * <p><b>It never reverts the drain.</b> A timer cannot tell an abandoned deploy from a careful
     * one, and at the moment it fired the node might hold a half-copied JAR, a stopped JVM, or a
     * {@code *.MISCONFIGURED} quarantine. Returning it to the pool then would send live traffic into
     * exactly the state the drain existed to prevent. The operator decides; this only makes sure
     * they are told.
     *
     * <p><b>Deliberately no {@code @SchedulerLock}</b>, against the project's usual rule. Every
     * instance must run this: each one is reporting on <em>itself</em>, and a cluster-wide lock
     * would mean only one node per interval could ever warn — most likely not the drained one. Same
     * reasoning as {@code DeferredRecoverySweeper} and {@code StanNodeLeaseService.heartbeat}.
     *
     * @return {@code true} if a warning was emitted (for tests; Spring ignores the value)
     */
    @Scheduled(fixedDelayString = "${gotechly.drain.warn-check-interval:PT1M}",
            initialDelayString = "${gotechly.drain.warn-check-interval:PT1M}")
    public boolean warnOnLongRunningDrain() {
        if (!isDraining()) {
            return false;
        }
        Instant now = Instant.now();
        if (Duration.between(stateSince, now).compareTo(warnAfter) < 0) {
            return false;
        }
        Instant last = lastWarnAt;
        if (last != null && Duration.between(last, now).compareTo(warnInterval) < 0) {
            return false;
        }
        lastWarnAt = now;
        LOG.warn("NODE_DRAIN_STALE instance={} drainedFor={} requestedBy={} quiet={} — this instance is "
                        + "NOT taking traffic and the cluster is running with reduced capacity. Either "
                        + "finish the deploy and restart it, or return it to service.",
                instanceInfo.getInstanceId(), Duration.between(stateSince, now), requestedBy, isQuiet());
        return true;
    }

    public DrainState getState() {
        return state.get();
    }

    public boolean isDraining() {
        return state.get() == DrainState.DRAINING;
    }

    // =========================================================================
    // In-flight accounting
    // =========================================================================

    /**
     * Records that a request has started. The caller <em>must</em> pass the returned token to
     * {@link #endRequest(long)} exactly once, when the request has genuinely finished — which for
     * an async (Mono-returning) controller is its {@code AsyncListener.onComplete}, not the return
     * of {@code doFilter}.
     */
    public long beginRequest() {
        return beginRequest(null, null, null, null);
    }

    /**
     * As {@link #beginRequest()}, recording what the request is and where it came from so a drained
     * node can say <em>what</em> it is still waiting on — and, for a request that arrived after the
     * drain, which route bypassed the load balancer.
     *
     * @param uri          path only. The query string is deliberately not taken: it can carry
     *                     account numbers and this ends up on an admin page and in the log.
     * @param forwardedFor the {@code X-Forwarded-For} header, if any — set by the Gateway, so its
     *                     presence separates "came through the Gateway" from a direct caller.
     */
    public long beginRequest(String method, String uri, String remoteAddr, String forwardedFor) {
        long id = sequence.incrementAndGet();
        InFlightRequest request = new InFlightRequest(System.currentTimeMillis(), method, uri, remoteAddr, forwardedFor);
        if (inFlight.put(id, request) == null) {
            inFlightCount.incrementAndGet();
        }
        if (isDraining()) {
            recordArrivalWhileDraining(request);
        }
        return id;
    }

    private void recordArrivalWhileDraining(InFlightRequest request) {
        long count = arrivalsWhileDraining.incrementAndGet();
        recentArrivals.addFirst(request.view(System.currentTimeMillis()));
        while (recentArrivals.size() > MAX_LISTED_REQUESTS) {
            recentArrivals.pollLast();
        }
        // Throttled: if the load balancer is not excluding this node, it is taking full traffic and a
        // line per request would flood the log. One line per interval names the latest offender and
        // the running total, which is all an operator needs to pick the cause.
        Instant now = Instant.now();
        Instant last = lastArrivalWarnAt;
        if (last == null || Duration.between(last, now).compareTo(ARRIVAL_WARN_INTERVAL) >= 0) {
            lastArrivalWarnAt = now;
            LOG.warn("NODE_DRAIN_ARRIVAL instance={} drainedFor={} arrivalsSinceDrain={} latest={} {} "
                            + "remote={} forwardedFor={} — a request reached this instance after it was "
                            + "drained. Arrivals within ~45s of the drain are the load balancer catching "
                            + "up; later ones mean this route does not consult /api/v1/admin/node/routable.",
                    instanceInfo.getInstanceId(), Duration.between(stateSince, now), count,
                    request.method(), request.uri(), request.remoteAddr(), request.forwardedFor());
        }
    }

    /** Idempotent: a token already removed is ignored, so a double-fire cannot drive the count negative. */
    public void endRequest(long id) {
        if (inFlight.remove(id) != null) {
            inFlightCount.decrementAndGet();
        }
    }

    public int getInFlightCount() {
        return inFlightCount.get();
    }

    // =========================================================================
    // Background work
    // =========================================================================

    /**
     * Background work running on this instance right now, keyed by an opaque token so two concurrent
     * runs of the same job (several recovery workers, say) each hold their own slot.
     */
    private final ConcurrentHashMap<Long, String> backgroundJobs = new ConcurrentHashMap<>();
    private final AtomicLong backgroundSequence = new AtomicLong();

    /**
     * Runs {@code body} unless this node is draining, tracking it as active for as long as it runs.
     *
     * <p>For background work that <b>re-fires on its own</b> and is safe to skip: a skipped cycle
     * costs nothing because the next one picks the work up, and on a draining node the peers are
     * doing it anyway.
     *
     * <p><b>Do not use this on a {@code @SchedulerLock}ed job.</b> ShedLock acquires the lock
     * <em>before</em> the method body runs, so a draining instance would take the cluster-wide lock
     * and then decline to do the work — and with {@code lockAtLeastFor = PT1M} on the EOD jobs it
     * holds that lock for a minute afterwards. Peers firing on the same cron instant have already
     * failed to acquire it, so the run would be lost for the night rather than moved to another
     * instance. Those jobs use {@link #trackBackgroundJob} instead and are never gated.
     *
     * @return {@code false} if the work was skipped because this node is draining
     */
    public boolean runIfRoutable(String name, Runnable body) {
        if (isDraining()) {
            LOG.debug("NODE_DRAIN skipping background job '{}' — instance is draining", name);
            return false;
        }
        trackBackgroundJob(name, body);
        return true;
    }

    /**
     * Runs {@code body} unconditionally, tracking it as active so a drained node does not report
     * itself quiet while it is still working. For work that must not be skipped — see the ShedLock
     * note on {@link #runIfRoutable}.
     */
    public void trackBackgroundJob(String name, Runnable body) {
        long token = backgroundSequence.incrementAndGet();
        backgroundJobs.put(token, name);
        try {
            body.run();
        } finally {
            backgroundJobs.remove(token);
        }
    }

    /** Names of the background jobs running right now; a name repeats once per concurrent run. */
    public List<String> getActiveBackgroundJobs() {
        return List.copyOf(backgroundJobs.values());
    }

    /**
     * Safe to swap a plugin JAR and restart: this node is refusing new work, no request is still
     * running, and no background job is executing plugin code.
     */
    public boolean isQuiet() {
        return isDraining() && inFlightCount.get() == 0 && backgroundJobs.isEmpty();
    }

    /** Age of the longest-running request still in flight, or {@code 0} when there are none. */
    public long getOldestRequestAgeMs() {
        Map.Entry<Long, InFlightRequest> oldest = inFlight.firstEntry();
        return oldest == null ? 0L : Math.max(0L, System.currentTimeMillis() - oldest.getValue().startMillis());
    }

    /** The requests running right now, oldest first, capped at {@link #MAX_LISTED_REQUESTS}. */
    public List<RequestView> getInFlightRequests() {
        long now = System.currentTimeMillis();
        List<RequestView> views = new ArrayList<>(Math.min(inFlightCount.get(), MAX_LISTED_REQUESTS));
        for (InFlightRequest request : inFlight.values()) {
            if (views.size() >= MAX_LISTED_REQUESTS) {
                break;
            }
            views.add(request.view(now));
        }
        return List.copyOf(views);
    }

    /** Requests that arrived since the current drain began, newest first. Empty while routable. */
    public List<RequestView> getRecentArrivalsWhileDraining() {
        return List.copyOf(recentArrivals);
    }

    public long getArrivalsWhileDraining() {
        return arrivalsWhileDraining.get();
    }

    // =========================================================================
    // Reporting
    // =========================================================================

    public Status status() {
        int count = inFlightCount.get();
        List<String> background = getActiveBackgroundJobs();
        return new Status(
                instanceInfo.getInstanceId(),
                state.get(),
                stateSince,
                Duration.between(stateSince, Instant.now()).toSeconds(),
                count,
                getOldestRequestAgeMs(),
                isDraining() && count == 0,
                background,
                isDraining() && count == 0 && background.isEmpty(),
                requestedBy,
                getInFlightRequests(),
                arrivalsWhileDraining.get(),
                getRecentArrivalsWhileDraining());
    }

    /** What the filter knows about a running request. Internal; {@link RequestView} is the wire shape. */
    private record InFlightRequest(long startMillis, String method, String uri, String remoteAddr,
                                   String forwardedFor) {
        RequestView view(long nowMillis) {
            return new RequestView(method, uri, remoteAddr, forwardedFor,
                    Instant.ofEpochMilli(startMillis), Math.max(0L, nowMillis - startMillis));
        }
    }

    /**
     * One request, as reported on the drain status.
     *
     * @param remoteAddr   the TCP peer. {@code 127.0.0.1} on a drained node is almost always a plugin
     *                     calling back to {@code core.base-url}; the Gateway's address means the load
     *                     balancer is still routing here.
     * @param forwardedFor {@code X-Forwarded-For}, when the request came through a proxy
     * @param ageMs        how long it had been running when the status was taken (for an arrival,
     *                     when it was recorded)
     */
    public record RequestView(String method, String uri, String remoteAddr, String forwardedFor,
                              Instant startedAt, long ageMs) {
    }

    /**
     * @param httpQuiet            no HTTP request is running and the node is draining. Kept
     *                             alongside {@code quiet} because it separates "the traffic has
     *                             stopped" from "a nightly archival is still going", which are very
     *                             different waits for an operator watching the page.
     * @param activeBackgroundJobs what is still running that is not an HTTP request.
     * @param quiet                <b>the field to gate a deploy on.</b> Draining, no requests, no
     *                             background work.
     * @param requestedBy          the portal user who set the current state.
     * @param inFlightRequests     what {@code inFlightHttp} is counting, oldest first, capped at
     *                             {@link #MAX_LISTED_REQUESTS}.
     * @param arrivalsWhileDraining requests received since the current drain began. Should stop
     *                             growing within one probe interval plus the LB cache TTL; if it keeps
     *                             climbing, something is routing here without consulting
     *                             {@code /routable}.
     * @param recentArrivalsWhileDraining the latest of those, newest first — the remote address is
     *                             what identifies the route.
     */
    public record Status(
            String instanceId,
            DrainState state,
            Instant stateSince,
            long stateForSeconds,
            int inFlightHttp,
            long oldestRequestAgeMs,
            boolean httpQuiet,
            List<String> activeBackgroundJobs,
            boolean quiet,
            String requestedBy,
            List<RequestView> inFlightRequests,
            long arrivalsWhileDraining,
            List<RequestView> recentArrivalsWhileDraining
    ) {
    }
}
