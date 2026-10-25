package com.dotechly.gotechly.core.service.inward;


import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineExecutor;
import com.dotechly.gotechly.core.pipeline.PipelineRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Runs the inbound pipeline for a linked-wallet operation under an operation-specific
 * {@code apiType} (e.g. {@code deposit}, {@code balance}).
 *
 * <p>Distinct from {@link com.dotechly.gotechly.core.service.plugin.validation.ReactiveValidationService}
 * because that entry point hard-codes {@code apiType = "validation"} and runs the outward-only
 * {@code InputsValidationService} (whose {@code @NotNull amount} check would reject Balance/Status
 * requests that carry no amount). Here the plugin has already built a pipeline-ready PaymentLogSPI,
 * so we resolve stages by the operation's apiType and execute directly.
 */
@Service
public class LinkedWalletPipelineService {

    private static final Logger LOG = LoggerFactory.getLogger(LinkedWalletPipelineService.class);

    private static final String DIRECTION = "inward";

    private final PipelineRegistry pipelineRegistry;
    private final PipelineExecutor pipelineExecutor;

    public LinkedWalletPipelineService(PipelineRegistry pipelineRegistry,
                                       PipelineExecutor pipelineExecutor) {
        this.pipelineRegistry = pipelineRegistry;
        this.pipelineExecutor = pipelineExecutor;
    }

    /**
     * Execute the inbound pipeline for {@code apiType} against the plugin-composed PaymentLogSPI.
     *
     * @param paymentLogSPI plugin-composed, pipeline-ready log (Details.paymentType must be set)
     * @param apiType       the operation's apiType (see {@code LinkedWalletOperation.apiType()})
     */
    public Mono<PaymentLogSPI> process(PaymentLogSPI paymentLogSPI, String apiType) {
        String paymentType = paymentLogSPI.getDetails().getPaymentType();
        PipelineContext context = new PipelineContext(paymentType, DIRECTION, apiType);
        List<PipelineStage> stages = pipelineRegistry.getPipeline(DIRECTION, paymentType, apiType);
        LOG.debug("Executing inward linked-wallet pipeline paymentType='{}' apiType='{}' with {} stage(s)",
                paymentType, apiType, stages.size());
        return pipelineExecutor.executeReactive(stages, context, paymentLogSPI);
    }
}
