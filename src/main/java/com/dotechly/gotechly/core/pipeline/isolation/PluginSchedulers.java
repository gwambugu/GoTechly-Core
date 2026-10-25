package com.dotechly.gotechly.core.pipeline.isolation;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

/**
 * One bounded thread pool per payment type, so a degraded provider can only exhaust its own.
 *
 * <p><b>The incident this exists to prevent.</b> Plugin handler calls are blocking — the SPI's
 * reactive default is {@code Mono.fromCallable(() -> paymentProcessRequest(log))} with no
 * {@code subscribeOn} of its own, so the call runs on whatever thread subscribed. In practice that
 * was the shared {@link Schedulers#boundedElastic()}: roughly ten threads per core, behind a queue
 * of <b>100,000</b>. When one provider's endpoint stopped answering, every call to it held a thread
 * for the length of its timeout, the pool saturated, and — because the queue is effectively
 * unbounded — work was accepted and buried rather than refused. Every other stage that offloads
 * queued behind it: {@code db-logger}, {@code db-update}, {@code aml-screening}, CBS postings,
 * {@code callback}, notifications, the rendezvous. All payment types stalled, not just the degraded
 * one, and the request threads waiting on them exhausted Tomcat.
 *
 * <p>A bounded queue is the half that matters. A pool that saturates and <em>rejects</em> fails the
 * one corridor in seconds; a pool that saturates and queues fails the platform in minutes.
 *
 * <p><b>What this does not cover.</b> A plugin that declares its own {@code subscribeOn} keeps it —
 * the operator closest to the source wins — so such a handler stays on whichever scheduler it named.
 * {@code RndpsLinkedAccountHandler} is the one in this tree that does. Everything reaching the
 * provider through the SPI defaults, which is the payment, validation, status, result and callback
 * path, is isolated here.
 */
@Component
@EnableConfigurationProperties(PluginIsolationProperties.class)
public class PluginSchedulers {

    private static final Logger LOG = LoggerFactory.getLogger(PluginSchedulers.class);

    private final PluginIsolationProperties properties;
    private final Map<String, Scheduler> byPaymentType = new ConcurrentHashMap<>();

    public PluginSchedulers(PluginIsolationProperties properties) {
        this.properties = properties;
    }

    /**
     * Runs {@code call} on the bulkhead for {@code paymentType}.
     *
     * <p>Wrap the provider call itself, innermost — {@code subscribeOn} affects the subscription, so
     * placing it against the source both moves the blocking work off the caller's thread and keeps
     * the stage's own error handling downstream of it.
     */
    public <T> Mono<T> isolate(String paymentType, Mono<T> call) {
        if (!properties.isEnabled()) {
            return call;
        }
        String type = normalise(paymentType);
        return call
                .subscribeOn(schedulerFor(type))
                .onErrorMap(PluginSchedulers::isRejection, ex -> overloaded(type, ex));
    }

    private Scheduler schedulerFor(String paymentType) {
        return byPaymentType.computeIfAbsent(paymentType, type -> {
            int threads = properties.threadCapFor(type);
            int queue = properties.queueCapFor(type);
            LOG.info("[ISOLATION] bulkhead for paymentType '{}': {} thread(s), queue {}, ttl {}s",
                    type, threads, queue, properties.getTtlSeconds());
            return Schedulers.newBoundedElastic(threads, queue, "plugin-" + type,
                    properties.getTtlSeconds(), true);
        });
    }

    /**
     * Reactor reports saturation as a {@link RejectedExecutionException}, sometimes wrapped. Checking
     * the cause chain rather than the top type keeps the translation working when it is.
     */
    private static boolean isRejection(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (c instanceof RejectedExecutionException) {
                return true;
            }
        }
        return false;
    }

    private PluginOverloadedException overloaded(String paymentType, Throwable cause) {
        // Loud on purpose: this is the corridor shedding load, and it is the signal that would have
        // named the culprit during the incident instead of leaving a platform-wide stall to diagnose.
        LOG.error("[ISOLATION] paymentType '{}' is saturated — {} thread(s) busy and {} queued; "
                        + "rejecting further work for this corridor. Other payment types are unaffected. "
                        + "Check the provider endpoint before raising the caps.",
                paymentType, properties.threadCapFor(paymentType), properties.queueCapFor(paymentType));
        return new PluginOverloadedException(paymentType, cause);
    }

    private static String normalise(String paymentType) {
        return paymentType == null || paymentType.isBlank() ? "UNKNOWN" : paymentType.trim();
    }

    /**
     * Disposing releases the threads. Without this a hot reload would leak a pool per reload — the
     * schedulers are keyed by payment type, not by plugin classloader, so they deliberately survive a
     * plugin reload and are torn down only with the application.
     */
    @PreDestroy
    public void shutdown() {
        byPaymentType.forEach((type, scheduler) -> {
            LOG.info("[ISOLATION] disposing bulkhead for paymentType '{}'", type);
            scheduler.dispose();
        });
        byPaymentType.clear();
    }
}
