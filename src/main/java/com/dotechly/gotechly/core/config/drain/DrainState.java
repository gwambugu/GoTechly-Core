package com.dotechly.gotechly.core.config.drain;

/**
 * Whether this Core instance is willing to receive new work from the Gateway load balancer.
 *
 * <p>Deliberately <em>not</em> persisted. A restarted node comes back {@link #ROUTABLE} — that is
 * the point of having drained it. Keeping a node out of the pool across a restart is a
 * configuration decision, not a drain.
 *
 * @see NodeDrainService
 */
public enum DrainState {

    /** Normal. The LB probe answers 200 and the instance stays in the pool. */
    ROUTABLE,

    /**
     * Refusing new work. The LB probe answers 503 and the instance is filtered out within one
     * probe interval; requests already accepted continue to run to completion.
     */
    DRAINING
}
