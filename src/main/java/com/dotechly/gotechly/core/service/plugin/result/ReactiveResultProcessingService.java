package com.dotechly.gotechly.core.service.plugin.result;

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

@Service
public class ReactiveResultProcessingService {

    private static final Logger LOG = LoggerFactory.getLogger(ReactiveResultProcessingService.class);

    private final PipelineRegistry pipelineRegistry;
    private final PipelineExecutor pipelineExecutor;

    public ReactiveResultProcessingService(PipelineRegistry pipelineRegistry, PipelineExecutor pipelineExecutor) {
        this.pipelineRegistry = pipelineRegistry;
        this.pipelineExecutor = pipelineExecutor;
    }

    public Mono<String> resultProcessingRequest(PaymentLogSPI paymentLogSPI, String result, String direction) {
        LOG.info("Reactive result-processing request: {}", paymentLogSPI);
        String paymentType = paymentLogSPI.getDetails().getPaymentType();
        PipelineContext context = new PipelineContext(paymentType, direction, "result-processing");
        context.setRawResult(result);
        List<PipelineStage> stages = pipelineRegistry.getPipeline(direction, paymentType, "result-processing");
        LOG.debug("Executing reactive {} result-processing pipeline for paymentType '{}' with {} stages",
                direction, paymentType, stages.size());
        return pipelineExecutor.executeReactive(stages, context, paymentLogSPI)
                .map(finalResult -> {
                    String resultResponse = context.getRawResultResponse();
                    if (resultResponse == null) {
                        LOG.warn("No provider response set by result handler for paymentType '{}'; returning empty acknowledgement", paymentType);
                        resultResponse = "";
                    }
                    return resultResponse;
                });
    }
}
