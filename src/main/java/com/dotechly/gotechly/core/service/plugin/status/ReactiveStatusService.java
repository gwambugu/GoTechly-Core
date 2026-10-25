package com.dotechly.gotechly.core.service.plugin.status;

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
public class ReactiveStatusService {

    private static final Logger LOG = LoggerFactory.getLogger(ReactiveStatusService.class);

    private final PipelineRegistry pipelineRegistry;
    private final PipelineExecutor pipelineExecutor;

    public ReactiveStatusService(PipelineRegistry pipelineRegistry, PipelineExecutor pipelineExecutor) {
        this.pipelineRegistry = pipelineRegistry;
        this.pipelineExecutor = pipelineExecutor;
    }

    public Mono<PaymentLogSPI> statusProcessingRequest(PaymentLogSPI paymentLogSPI, String direction) {
        LOG.info("Reactive status-processing request: {}", paymentLogSPI);

        String paymentType = paymentLogSPI.getDetails().getPaymentType();
        PipelineContext context = new PipelineContext(paymentType, direction, "status-processing");
        List<PipelineStage> stages = pipelineRegistry.getPipeline(direction, paymentType, "status-processing");

        LOG.debug("Executing reactive {} status-processing pipeline for paymentType '{}' with {} stages",
                direction, paymentType, stages.size());
        return pipelineExecutor.executeReactive(stages, context, paymentLogSPI);
    }
    public String statusProcessingRequestAsync(PaymentLogSPI paymentLogSPI, String direction) {
        LOG.info("Async status-processing request: {}", paymentLogSPI);

        String paymentType = paymentLogSPI.getDetails().getPaymentType();
        PipelineContext context = new PipelineContext(paymentType, direction, "status-processing");
        List<PipelineStage> stages = pipelineRegistry.getPipeline(direction, paymentType, "status-processing");

        LOG.debug("Executing reactive {} status-processing pipeline for paymentType '{}' with {} stages",
                direction, paymentType, stages.size());

         pipelineExecutor.executeReactive(stages, context, paymentLogSPI);
        String asyncResponse = (String) context.getAttribute("async-response");
        return asyncResponse != null ? asyncResponse : "REQUEST_ACKNOWLEDGED";
    }
}
