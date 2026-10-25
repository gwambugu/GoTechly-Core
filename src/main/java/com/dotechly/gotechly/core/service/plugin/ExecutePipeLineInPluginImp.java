package com.dotechly.gotechly.core.service.plugin;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineExecutor;
import com.dotechly.gotechly.core.pipeline.PipelineRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.ExecutePipeline;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ExecutePipeLineInPluginImp implements ExecutePipeline {
    private static final Logger LOG = LoggerFactory.getLogger(ExecutePipeLineInPluginImp.class);
    private final PipelineExecutor pipelineExecutor;
    private final PipelineRegistry pipelineRegistry;

    public ExecutePipeLineInPluginImp(PipelineExecutor pipelineExecutor, PipelineRegistry pipelineRegistry) {
        this.pipelineExecutor = pipelineExecutor;
        this.pipelineRegistry = pipelineRegistry;
    }

    @Override
    public Mono<PaymentLogSPI> execute(PaymentLogSPI paymentLogSPI, String direction, String pipelinePhase) {
        String paymentType = paymentLogSPI.getDetails().getPaymentType();
        PipelineContext context = new PipelineContext(paymentType, direction, pipelinePhase);
        List<PipelineStage> stages = pipelineRegistry.getPipeline(direction,paymentType , pipelinePhase);

        LOG.debug("Executing reactive {} validation pipeline for paymentType '{}' with {} stages",
                direction, paymentType, stages.size());
        return pipelineExecutor.executeReactive(stages, context, paymentLogSPI);
    }
}
