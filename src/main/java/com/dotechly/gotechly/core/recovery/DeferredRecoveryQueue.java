package com.dotechly.gotechly.core.recovery;

import com.dotechly.gotechly.core.config.drain.NodeDrainService;
import com.dotechly.gotechly.core.domain.DeferredRecovery;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The in-memory working set: a bounded, time-ordered queue of recovery rows and the worker threads
 * that drain it.
 *
 * <p>Three properties are worth stating outright, because each one is a decision that could have gone
 * the other way:
 *
 * <p><b>It is a cache, not a ledger.</b> Every task in here has a {@code deferred_recovery} row
 * behind it. Losing the queue to a restart loses nothing; the sweeper reads the rows back. This is
 * what lets the queue be bounded without ever rejecting work.
 *
 * <p><b>Full means "not right now", never "no".</b> {@link #offer} returns false and the row simply
 * stays {@code PENDING} for the sweeper. Rejecting the enqueue instead — the obvious reading of a
 * bounded queue — would drop a transaction that may already be posted in core banking while the
 * scheme holds an answer that contradicts it, and nothing would ever look for it again. The capacity
 * is a backpressure signal to alert on, not an admission gate.
 *
 * <p><b>Delay, not FIFO.</b> Each task carries its own {@code nextAttemptAt}; a
 * {@link DelayQueue} releases it when that time arrives. Putting a failed task at the back of a plain
 * FIFO looks like backoff but is not — with a queue of two it retries almost immediately, and a
 * core-banking outage turns into a hot loop against a host that is already unwell.
 */
@Component
public class DeferredRecoveryQueue {

    private static final Logger LOG = LoggerFactory.getLogger(DeferredRecoveryQueue.class);

    private final RecoveryProperties properties;
    private final DeferredRecoveryService service;
    private final DeferredRecoveryEngine engine;
    private final NodeDrainService drainService;

    private final DelayQueue<RecoveryTask> queue = new DelayQueue<>();
    /**
     * Ids currently in the queue or being worked, so the sweeper's periodic pass cannot enqueue a
     * second copy of a task a worker already holds.
     */
    private final Set<Long> tracked = ConcurrentHashMap.newKeySet();

    private final AtomicInteger highWaterMark = new AtomicInteger();
    private final AtomicLong rejectedOffers = new AtomicLong();
    private final AtomicLong attemptsRun = new AtomicLong();
    private final AtomicBoolean running = new AtomicBoolean();
    private final String instanceId = resolveInstanceId();

    private ExecutorService workers;

    public DeferredRecoveryQueue(RecoveryProperties properties, DeferredRecoveryService service,
                                 DeferredRecoveryEngine engine, NodeDrainService drainService) {
        this.properties = properties;
        this.service = service;
        this.engine = engine;
        this.drainService = drainService;
    }

    /**
     * Started on {@code ApplicationReadyEvent} rather than {@code @PostConstruct}: a worker can reach
     * core banking and the pipeline registry within microseconds of starting, and neither is ready
     * while beans are still being wired.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!properties.isEnabled()) {
            LOG.info("[RECOVERY] disabled (gotechly.recovery.enabled=false) — no workers started");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        int count = Math.max(1, properties.getWorkers());
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger n = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "recovery-worker-" + n.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
        workers = Executors.newFixedThreadPool(count, factory);
        for (int i = 0; i < count; i++) {
            workers.submit(this::workLoop);
        }
        LOG.info("[RECOVERY] started {} worker(s), queue capacity {}, instance '{}'",
                count, properties.getQueueCapacity(), instanceId);
    }

    @PreDestroy
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (workers != null) {
            workers.shutdownNow();
        }
        LOG.info("[RECOVERY] workers stopped; {} task(s) left in memory (their rows remain PENDING)",
                queue.size());
    }

    /**
     * Offers a row to the working set.
     *
     * @return {@code false} when the queue is at capacity or the row is already tracked. Neither is
     *         an error: the durable row is untouched and the sweeper will bring it back.
     */
    public boolean offer(DeferredRecovery row) {
        if (!running.get() || row.getId() == null) {
            return false;
        }
        if (!tracked.add(row.getId())) {
            return false;
        }
        if (queue.size() >= properties.getQueueCapacity()) {
            tracked.remove(row.getId());
            long total = rejectedOffers.incrementAndGet();
            // Loud, because a full recovery queue means core banking has been failing for a while.
            LOG.warn("[RECOVERY] working set at capacity ({}) — requestId '{}' stays PENDING for the "
                            + "sweeper. {} deferral(s) so far.",
                    properties.getQueueCapacity(), row.getRequestId(), total);
            return false;
        }
        queue.offer(new RecoveryTask(row.getId(), row.getRequestId(), row.getNextAttemptAt()));
        int size = queue.size();
        highWaterMark.accumulateAndGet(size, Math::max);
        return true;
    }

    private void workLoop() {
        while (running.get()) {
            RecoveryTask task = null;
            try {
                task = queue.poll(5, TimeUnit.SECONDS);
                if (task == null) {
                    continue;
                }
                // Draining: drop the task WITHOUT claiming it. A repair runs plugin handler stages,
                // and this node is about to have its plugins replaced. Dropping is safe by exactly
                // the same argument as the "claimed elsewhere" branch below — the row was never
                // claimed, so it stays due and a peer picks it up; the finally block releases it
                // from `tracked` so our own sweeper can re-offer it after the undrain.
                if (drainService.isDraining()) {
                    LOG.debug("[RECOVERY] requestId '{}' released — this instance is draining", task.requestId());
                    continue;
                }
                // The claim is what makes multi-instance safe: two instances can both hold the task in
                // memory, but only one wins the row, and the loser drops it without touching core
                // banking.
                if (!service.claim(task.id(), instanceId)) {
                    LOG.debug("[RECOVERY] requestId '{}' claimed elsewhere — skipping", task.requestId());
                    continue;
                }
                final RecoveryTask current = task;
                service.find(task.requestId())
                        .filter(row -> !row.isTerminal())
                        .ifPresent(row -> {
                            attemptsRun.incrementAndGet();
                            // Tracked, not gated: by here the row is claimed and core banking may
                            // already have been touched, so the attempt must finish. Tracking is
                            // what stops a drain from reporting quiet while it runs.
                            drainService.trackBackgroundJob(
                                    "deferred-recovery-attempt:" + current.requestId(),
                                    () -> engine.attempt(row));
                        });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                LOG.error("[RECOVERY] worker error: {}", e.getMessage(), e);
            } finally {
                if (task != null) {
                    // Released only after the attempt has reached a durable state, so the sweeper
                    // cannot pick the row up while a worker still holds it.
                    tracked.remove(task.id());
                }
            }
        }
    }

    /** Live numbers for the monitoring endpoint. */
    public QueueMetrics metrics() {
        return new QueueMetrics(running.get(), queue.size(), properties.getQueueCapacity(),
                highWaterMark.get(), rejectedOffers.get(), attemptsRun.get(),
                properties.getWorkers(), instanceId);
    }

    public String instanceId() {
        return instanceId;
    }

    public record QueueMetrics(boolean running, int queued, int capacity, int highWaterMark,
                               long deferredOffers, long attemptsRun, int workers, String instanceId) {
    }

    /** A queued attempt. Ordered by when it becomes eligible, not by when it was enqueued. */
    private record RecoveryTask(Long id, String requestId, Instant dueAt) implements Delayed {

        @Override
        public long getDelay(TimeUnit unit) {
            long millis = Duration.between(Instant.now(), dueAt).toMillis();
            return unit.convert(millis, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
        }
    }

    private static String resolveInstanceId() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "unknown-host";
        }
        return host + ":" + ProcessHandle.current().pid();
    }
}
