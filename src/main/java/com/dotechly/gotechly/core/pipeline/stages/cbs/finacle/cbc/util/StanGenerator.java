package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates DE11 (system trace audit number) so that no two Core instances can produce the same
 * value.
 *
 * <p><b>Why this is not a cosmetic concern.</b> Every mux keys on {@code 12,37} (see
 * {@code deploy/*mux*.xml}) and every participant builds the RRN as {@code stan + HHmmss}, with DE12
 * as {@code yyMMddHHmmss}. Two requests that draw the same STAN in the same second therefore carry an
 * identical {@code (DE12, DE37)} pair, and {@code QMUX} delivers the reply to whichever waiter it
 * matches first: one transaction receives another's response, and the other times out and halts
 * {@code cbc-payment-unconfirmed} on a posting core banking actually answered.
 *
 * <p>The previous generators made that collision likely rather than remote:
 * <ul>
 *   <li>{@code last 6 digits of System.currentTimeMillis()} — colliding on the STAN <em>means</em>
 *       colliding on the millisecond, hence on {@code HHmmss} and DE12 too. The collision was always
 *       total, never partial.</li>
 *   <li>{@code random.nextInt(1_000_000)} — by the birthday bound a single instance has roughly even
 *       odds of some collision within ~1,200 messages, no cluster required.</li>
 *   <li>{@code UniqueNumberGenerator} — a {@code static AtomicInteger}, so it serialised one JVM and
 *       said nothing at all about the other two.</li>
 * </ul>
 *
 * <p><b>The scheme.</b> Six digits is a space of 1,000,000. One decimal digit is a node ordinal and
 * the remaining five are a per-node counter, so each instance owns a disjoint block of
 * {@value #PER_NODE} values:
 *
 * <pre>
 *   node 0 -&gt; 000000..099999      node 1 -&gt; 100000..199999      ...      node 9 -&gt; 900000..999999
 * </pre>
 *
 * Uniqueness across the cluster is then <b>arithmetic, not probabilistic</b>: two instances cannot
 * collide because their ranges do not overlap. The cost on the CBS path is one
 * {@code getAndIncrement} — no DB round trip, no lock, no new failure mode on the money path. The
 * ordinal itself is leased from {@code cbs_node_lease} once at startup; see
 * {@code StanNodeLeaseService}.
 *
 * <p><b>Wrap-around is safe.</b> A node reuses a value every {@value #PER_NODE} messages, and a reuse
 * only collides if the earlier request is still in flight (the channel timeout is 300s) <em>and</em>
 * falls in the same wall-clock second, because the RRN carries {@code HHmmss}. That requires 100,000
 * messages in one second on one node. Five digits is sufficient; widening the counter at the expense
 * of the node ordinal would buy nothing and cap the cluster instead.
 *
 * <p><b>The counter starts at a random offset, not zero.</b> On a restart an instance reclaims its
 * own ordinal (the lease keys on {@code host:port}), and starting from zero would replay exactly the
 * values it had just used — against requests still in flight on a mux that has not yet timed them
 * out. The offset makes the restart land elsewhere in the ring.
 */
public final class StanGenerator {

    /** DE11 is six digits. */
    public static final int SPACE = 1_000_000;

    /** Node ordinals, one decimal digit. Ten is far more than the three instances deployed. */
    public static final int NODES = 10;

    /** Values owned by each node. */
    public static final int PER_NODE = SPACE / NODES;

    private final int nodeId;
    private final AtomicLong counter;

    public StanGenerator(int nodeId) {
        this(nodeId, ThreadLocalRandom.current().nextInt(PER_NODE));
    }

    /** Deterministic start, for tests. */
    StanGenerator(int nodeId, long start) {
        if (nodeId < 0 || nodeId >= NODES) {
            throw new IllegalArgumentException(
                    "STAN node ordinal must be 0.." + (NODES - 1) + ", got " + nodeId);
        }
        this.nodeId = nodeId;
        this.counter = new AtomicLong(start);
    }

    /** The next STAN for this instance, always exactly six digits. */
    public String next() {
        // floorMod rather than %: the counter is unbounded and would go negative on overflow, and a
        // negative remainder would put the value outside this node's block.
        long seq = Math.floorMod(counter.getAndIncrement(), (long) PER_NODE);
        return String.format("%06d", nodeId * (long) PER_NODE + seq);
    }

    public int nodeId() {
        return nodeId;
    }

    /** The half-open range this node owns, for logging. */
    public String range() {
        return String.format("%06d..%06d", nodeId * PER_NODE, (nodeId + 1) * PER_NODE - 1);
    }
}
