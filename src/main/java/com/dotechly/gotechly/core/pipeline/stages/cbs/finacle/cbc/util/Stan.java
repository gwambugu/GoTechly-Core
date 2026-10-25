package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util;


import java.util.concurrent.atomic.AtomicReference;

/**
 * The one place ISO 8583 message builders get a DE11 from.
 *
 * <p>A static holder rather than an injected bean because the callers are jPOS participants: Q2
 * instantiates them reflectively, with no Spring DI and no constructor of our choosing. The
 * alternative already in this package — {@code applicationContext.getBean(...)} per call — costs a
 * bean lookup on the posting path and gives the same coupling.
 *
 * <p>{@link com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.node.StanNodeLeaseService} installs the generator from its {@code @PostConstruct}, which
 * Spring completes during context refresh; {@code Q2Lifecycle} runs at
 * {@code Integer.MAX_VALUE} and so deploys nothing until afterwards. A participant therefore cannot
 * observe an uninstalled holder in a started Core, and the lease service refuses startup if it
 * cannot claim an ordinal — there is no half-configured state to degrade into.
 */
public final class Stan  {

    private static final AtomicReference<StanGenerator> INSTALLED = new AtomicReference<>();

    private Stan() {
    }

    /**
     * Publishes the generator for the ordinal this instance holds. Called once at startup, and again
     * if the lease is lost and a different ordinal is claimed.
     */
    public static void install(StanGenerator generator) {
        INSTALLED.set(generator);
    }

    /** The ordinal in force, or {@code null} before installation. Diagnostics only. */
    public static StanGenerator installed() {
        return INSTALLED.get();
    }

    /**
     * The next DE11 for this instance, always six digits.
     *
     * @throws IllegalStateException if no ordinal has been installed — a wiring error, not a runtime
     *         condition. Guessing a value here is the exact bug this class exists to remove, so it
     *         fails loudly instead.
     */
    public static String next() {
        StanGenerator generator = INSTALLED.get();
        if (generator == null) {
            throw new IllegalStateException(
                    "No STAN node ordinal installed — StanNodeLeaseService has not claimed a lease "
                            + "from cbs_node_lease. A STAN generated without one cannot be guaranteed "
                            + "unique across the cluster.");
        }
        return generator.next();
    }

}
