package com.dotechly.gotechly.core.service;

import com.dotechly.gotechly.core.config.RendezvousProperties;
import com.dotechly.gotechly.core.config.cluster.HazelcastConfiguration;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.rendezvous.PaymentRendezvous;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
/**
 * Bridges the sync-over-async gap for payment processing.
 * <p>
 * A caller invokes {@link #park(String)} to wait for an asynchronous result
 * identified by {@code requestId}. When the result arrives (e.g., via a plugin
 * webhook callback), {@link #complete(String, PaymentLogSPI)} delivers it to
 * the parked caller.
 * <p>
 * Uses a two-tier strategy:
 * <ul>
 *   <li><b>Local (in-process)</b> — a {@code ConcurrentHashMap} of Reactor
 *       {@link Sinks.One} instances. Zero-latency when the webhook hits the
 *       same Core instance that parked the request.</li>
 *   <li><b>Hazelcast (cross-instance)</b> — a self-expiring {@code IMap}
 *       ({@link HazelcastConfiguration#RENDEZVOUS_MAP}). {@link #complete} on the
 *       instance that received the webhook {@code put}s the result; an
 *       {@link EntryAddedListener}/{@link EntryUpdatedListener} on the instance
 *       that parked the request fires <em>push-style</em> (no polling) and emits
 *       into the parked sink. All Hazelcast errors are caught and logged — if the
 *       cluster is unavailable the local path still works for same-instance
 *       traffic and cross-instance requests degrade to a timeout.</li>
 * </ul>
 * This is the Redis/Valkey-free replacement for the previous poll-based design:
 * the embedded Hazelcast cluster ({@link HazelcastConfiguration}) already backs
 * config-refresh fan-out and idempotency dedup, so rendezvous reuses it rather
 * than carrying a separate Redis dependency.
 */
@Component
public class PaymentRendezvousImp implements PaymentRendezvous {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentRendezvousImp.class);

    private final ConcurrentMap<String, Sinks.One<PaymentLogSPI>> localWaiters = new ConcurrentHashMap<>();
    private final IMap<String, String> store;
    private final ObjectMapper objectMapper;
    private final RendezvousProperties properties;

    public PaymentRendezvousImp(HazelcastInstance hazelcast,
                                RendezvousProperties properties) {
        this.store = hazelcast.getMap(HazelcastConfiguration.RENDEZVOUS_MAP);
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        this.objectMapper.findAndRegisterModules();
    }

    @PostConstruct
    void registerClusterListener() {
        // includeValue=true so the event carries the serialized result and the listener never has to
        // read the map back. Fires cluster-wide (including locally), but only the member holding the
        // matching local waiter acts — every other member's lookup misses and is ignored.
        store.addEntryListener(new ResultListener(), true);
    }

    /**
     * Parks the current reactive chain until a result for {@code requestId}
     * arrives via {@link #complete}, or the configured timeout elapses.
     *
     * @return the result if delivered in time, or {@code Mono.empty()} on timeout
     *         or if the cluster is unavailable and no same-instance completion occurs
     */
    public Mono<PaymentLogSPI> park(String requestId) {
        Sinks.One<PaymentLogSPI> sink = Sinks.one();
        localWaiters.put(requestId, sink);

        // Cover the race where complete() landed on another instance and wrote the result before this
        // member registered its waiter (the entry listener would have fired with no waiter to notify).
        // Done after the sink is registered, so a result arriving concurrently is delivered by the
        // listener instead — the Sinks.One ignores the redundant second emit.
        Mono<PaymentLogSPI> preArrived = Mono.fromCallable(() -> store.remove(keyFor(requestId)))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Mono.empty())
                .flatMap(json -> {
                    PaymentLogSPI r = deserialize(json);
                    return r == null ? Mono.empty() : Mono.just(r);
                });

        return Flux.merge(sink.asMono(), preArrived).next()
                .timeout(waitTimeout())
                .onErrorResume(e -> {
                    if (!(e instanceof TimeoutException)) {
                        LOG.warn("Rendezvous error for requestId '{}': {}", requestId, e.getMessage());
                    }
                    return Mono.empty();
                })
                .doFinally(signal -> cleanup(requestId));
    }

    /**
     * Delivers the asynchronous result for {@code requestId}, unblocking any
     * caller parked via {@link #park}.
     * <p>
     * Same-instance delivery is immediate (local sink). Cross-instance delivery
     * writes the result to the cluster {@code IMap} so the parking member's entry
     * listener picks it up.
     */
    @Override
    public void complete(String requestId, PaymentLogSPI result) {
        LOG.info("Complete Rendezvous requestId '{}'", requestId);
        // Fast path: the waiter is on this instance — deliver in-process and skip the cluster entirely.
        // A requestId is parked by exactly one caller, so a local hit means no remote member is waiting.
        Sinks.One<PaymentLogSPI> sink = localWaiters.remove(requestId);
        if (sink != null) {
            LOG.info("sink !=null {}", requestId);
            sink.tryEmitValue(result);
            return;
        }

        // Cross-instance: hand the result off via the cluster map; the parking member's listener fires.
        String json = serialize(result);
        if (json != null) {
            try {
                store.put(keyFor(requestId), json, properties.getKeyTtlSeconds(), TimeUnit.SECONDS);
                LOG.info("Rendezvous result written to cluster for requestId '{}'", requestId);
            } catch (Exception e) {
                LOG.warn("Hazelcast unavailable for rendezvous complete (requestId '{}'): {}",
                        requestId, e.getMessage());
            }
        }
    }

    /**
     * Checks whether a rendezvous slot exists for the given {@code requestId}
     * (i.e., someone on this instance is waiting for this result).
     */
    public boolean hasWaiter(String requestId) {
        return localWaiters.containsKey(requestId);
    }

    // ── internal ────────────────────────────────────────────────────────

    /** Bridges a cluster {@code IMap} write into the local parked sink, if this member owns the waiter. */
    private void deliverFromCluster(String key, String json) {
        Sinks.One<PaymentLogSPI> sink = localWaiters.remove(requestIdFor(key));
        if (sink == null) {
            return;   // not our waiter — another member parked this, or it already completed/timed out
        }
        PaymentLogSPI result = deserialize(json);
        if (result != null) {
            sink.tryEmitValue(result);
        }
        // Consumed — drop the entry now rather than waiting for TTL. Async so we never block the
        // Hazelcast event thread this listener runs on; the map TTL is the backstop if it fails.
        store.removeAsync(key);
    }

    private final class ResultListener
            implements EntryAddedListener<String, String>, EntryUpdatedListener<String, String> {
        @Override
        public void entryAdded(EntryEvent<String, String> event) {
            deliverFromCluster(event.getKey(), event.getValue());
        }
        @Override
        public void entryUpdated(EntryEvent<String, String> event) {
            deliverFromCluster(event.getKey(), event.getValue());
        }
    }

    private void cleanup(String requestId) {
        localWaiters.remove(requestId);
        try {
            store.remove(keyFor(requestId));
        } catch (Exception e) {
            // Best-effort; the map TTL reclaims the entry regardless.
        }
    }

    private String keyFor(String requestId) {
        return properties.getKeyPrefix() + requestId;
    }

    private String requestIdFor(String key) {
        String prefix = properties.getKeyPrefix();
        return key.startsWith(prefix) ? key.substring(prefix.length()) : key;
    }

    private Duration waitTimeout() {
        return Duration.ofSeconds(properties.getWaitTimeoutSeconds());
    }

    private String serialize(PaymentLogSPI result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to serialize rendezvous result: {}", e.getMessage());
            return null;
        }
    }

    private PaymentLogSPI deserialize(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, PaymentLogSPI.class);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to deserialize rendezvous result: {}", e.getMessage());
            return null;
        }
    }
}
