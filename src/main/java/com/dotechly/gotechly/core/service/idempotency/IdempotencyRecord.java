package com.dotechly.gotechly.core.service.idempotency;

import java.io.Serializable;

/**
 * Cluster-replicated record of a request seen under a given {@code Idempotency-Key}.
 * Held in a Hazelcast {@code IMap} with TTL — the Redis-free dedup store for POST-safe retries.
 *
 * @param status      {@link #IN_FLIGHT} while the original request is processing, {@link #DONE}
 *                    once a response is captured
 * @param httpStatus  captured HTTP status of the original response (0 while in-flight)
 * @param contentType captured response content type (nullable)
 * @param body        captured response body (nullable while in-flight)
 * @param createdAt   epoch millis the key was first claimed
 */
public record IdempotencyRecord(String status,
                                int httpStatus,
                                String contentType,
                                String body,
                                long createdAt) implements Serializable {

    public static final String IN_FLIGHT = "IN_FLIGHT";
    public static final String DONE = "DONE";

    public boolean isInFlight() {
        return IN_FLIGHT.equals(status);
    }

    public boolean isDone() {
        return DONE.equals(status);
    }
}
