package com.dotechly.gotechly.core.pipeline;

import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * A single stage in the Core payment processing pipeline.
 * Implementations are Spring {@code @Component} beans discovered via DI.
 * The pipeline executor calls stages sequentially in configured order.
 */
public interface PipelineStage {

    /**
     * Unique name of this stage. Must match the name used in YAML config.
     * Convention: lowercase-hyphenated (e.g., "aml-screening", "plugin-handler").
     */
    String name();

    /**
     * Default ordering when YAML config does not specify explicit stage order.
     * Lower values execute first. The plugin-handler stage uses 1000.
     * Infrastructure stages use values below 1000 (pre-plugin) or above 1000 (post-plugin).
     */
    default int defaultOrder() { return 1000; }

    /**
     * Synchronous execution. Receives the current {@link PaymentLogSPI},
     * may inspect/modify it, and returns it for the next stage.
     *
     * @param context pipeline context with metadata about the current execution
     * @param request the current payment request
     * @return the (possibly modified) payment request for the next stage
     * @throws PipelineHaltException to short-circuit the pipeline. A stage delegating to a
     *         plugin handler may instead see SPI's
     *         {@link com.dotechly.gotechly.spi.service.util.common.PluginHaltException},
     *         which the executor translates into the same thing — let it propagate rather
     *         than folding it into a generic error response
     */
    PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request);

    /**
     * Reactive execution. Default wraps the sync method and runs it on
     * {@link Schedulers#boundedElastic()}.
     *
     * <p><b>The offload is not optional.</b> {@link #execute} is a blocking contract — the stages
     * relying on this default post to core banking over jPOS, write to the database, or call a
     * provider — and without {@code subscribeOn} the callable runs on whichever thread subscribed,
     * which on a WebFlux request is a Netty event loop. Netty pins connections to loops, so
     * blocking one for the length of a CBS posting (up to
     * {@code gotechly.cbs.cbc.response-timeout-ms}, 20s) stalls every unrelated request assigned
     * to that loop — portal APIs, other corridors, and the health endpoint the Gateway's
     * load balancer probes to decide whether this instance is alive.
     *
     * <p>Because {@code PipelineExecutor.executeReactive} chains stages with {@code flatMap}, every
     * stage downstream of the first offloaded one also runs off the event loop.
     *
     * <p>Override this <em>only</em> for genuinely non-blocking implementations — a stage returning
     * a {@code Mono} from a reactive client, or delegating to a plugin's own reactive method. An
     * override that wraps blocking work in {@code Mono.fromCallable} must carry its own
     * {@code subscribeOn}: it does not inherit this one.
     */
    default Mono<PaymentLogSPI> executeReactive(PipelineContext context, PaymentLogSPI request) {
        return Mono.fromCallable(() -> execute(context, request))
                .subscribeOn(Schedulers.boundedElastic());
    }

}