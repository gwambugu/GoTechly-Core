package com.dotechly.gotechly.core.service.idempotency;


import com.dotechly.gotechly.core.config.cluster.HazelcastConfiguration;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Cluster-wide idempotency / dedup for POST-safe retries, backed by a Hazelcast {@code IMap}.
 * <p>
 * This replaces a Redis-backed dedup store. {@link IMap#putIfAbsent} is atomic across the whole
 * cluster (serialized on the partition owner), so two instances racing the same
 * {@code Idempotency-Key} can never both win — exactly one claims it and processes the request;
 * the other observes the in-flight / completed record. Entries self-expire via the map's TTL
 * (configured in {@link HazelcastConfiguration}).
 */
@Service
public class IdempotencyService {

    private static final Logger LOG = LoggerFactory.getLogger(IdempotencyService.class);

    private final IMap<String, IdempotencyRecord> store;


    public IdempotencyService(HazelcastInstance hazelcast) {
        this.store = hazelcast.getMap(HazelcastConfiguration.IDEMPOTENCY_MAP);
    }

    /**
     * Atomically claim {@code key} for first-time processing.
     *
     * @return {@link Optional#empty()} if the caller now owns processing (key was unseen);
     *         otherwise the existing record (in-flight or completed) for a duplicate request
     */
    public Optional<IdempotencyRecord> begin(String key) {
        IdempotencyRecord inFlight = new IdempotencyRecord(
                IdempotencyRecord.IN_FLIGHT, 0, null, null, System.currentTimeMillis());
        IdempotencyRecord prior = store.putIfAbsent(key, inFlight);
        if (prior == null) {
            LOG.debug("Idempotency key '{}' claimed (first request).", key);
            return Optional.empty();
        }
        LOG.debug("Idempotency key '{}' is a duplicate (status={}).", key, prior.status());
        return Optional.of(prior);
    }

    /** Store the captured response so future duplicates can be replayed. */
    public void complete(String key, int httpStatus, String contentType, String body) {
        store.put(key, new IdempotencyRecord(
                IdempotencyRecord.DONE, httpStatus, contentType, body, System.currentTimeMillis()));
        LOG.debug("Idempotency key '{}' completed (httpStatus={}).", key, httpStatus);
    }

    /**
     * Release a claimed key so a genuine retry can re-process — used when the original request
     * failed and produced no authoritative response worth replaying.
     */
    public void release(String key) {
        store.remove(key);
        LOG.debug("Idempotency key '{}' released for retry.", key);
    }
}
