package com.dotechly.gotechly.core.service.plugin.validation;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineExecutor;
import com.dotechly.gotechly.core.pipeline.PipelineRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.core.pipeline.preprocess.outward.validation.InputsValidationService;
import com.dotechly.gotechly.spi.service.dto.payment.Details;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.util.common.validation.ValidationResultSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ReactiveValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(ReactiveValidationService.class);

    private final PipelineRegistry pipelineRegistry;
    private final PipelineExecutor pipelineExecutor;
    private final InputsValidationService inputsValidationService;

    public ReactiveValidationService(PipelineRegistry pipelineRegistry,
                                     PipelineExecutor pipelineExecutor,
                                     InputsValidationService inputsValidationService) {
        this.pipelineRegistry = pipelineRegistry;
        this.pipelineExecutor = pipelineExecutor;
        this.inputsValidationService = inputsValidationService;

    }

    public Mono<PaymentLogSPI> validateRequest(PaymentLogSPI paymentLogSPI, String direction) {
        LOG.info("Reactive validation request: {}", paymentLogSPI);
        ValidationResultSPI validationResultSPI = inputsValidationService.validate(paymentLogSPI,direction);
        if(!validationResultSPI.isValid()){//Not Valid
            LOG.error("Validation of inputs failed. {} {}",validationResultSPI.getErrorCode(),validationResultSPI.getErrorMsg());
            // Enrich the existing Details rather than replacing them: a fresh Details drops
            // paymentType/requestType/requestId, which loses the correlation id for the caller and
            // leaves protocol adapters unable to tell which message shape to reject in.
            Details details = paymentLogSPI.getDetails() == null ? new Details() : paymentLogSPI.getDetails();
            details.setResponseCode(validationResultSPI.getErrorCode());
            details.setStatusResponseCode(validationResultSPI.getErrorMsg());
            paymentLogSPI.setDetails(details);
            return Mono.just(paymentLogSPI);
        }
        String paymentType = paymentLogSPI.getDetails().getPaymentType();
        PipelineContext context = new PipelineContext(paymentType, direction, "validation");
        List<PipelineStage> stages = pipelineRegistry.getPipeline(direction, paymentType, "validation");

        LOG.debug("Executing reactive {} validation pipeline for paymentType '{}' with {} stages",
                direction, paymentType, stages.size());

        return pipelineExecutor.executeReactive(stages, context, paymentLogSPI);
    }
    public String validateRequestAsync(PaymentLogSPI paymentLogSPI, String direction) {
        LOG.info("Async validation request: {}", paymentLogSPI);
        ValidationResultSPI validationResultSPI = inputsValidationService.validate(paymentLogSPI,direction);
        if (!validationResultSPI.isValid()) {
            LOG.error("Validation of inputs failed. {} {}",
                    validationResultSPI.getErrorCode(), validationResultSPI.getErrorMsg());
            return "{\"errorCode\":\"" + validationResultSPI.getErrorCode()
                    + "\",\"errorMessage\":\"" + validationResultSPI.getErrorMsg() + "\"}";
        }

        String paymentType = paymentLogSPI.getDetails().getPaymentType();
        PipelineContext context = new PipelineContext(paymentType, direction, "async-validation");
        List<PipelineStage> stages = pipelineRegistry.getPipeline(direction, paymentType, "async-validation");

        LOG.debug("Executing async {} validation pipeline for paymentType '{}' with {} stages",
                direction, paymentType, stages.size());

        pipelineExecutor.execute(stages, context, paymentLogSPI);

        String asyncResponse = (String) context.getAttribute("async-response");
        return asyncResponse != null ? asyncResponse : "REQUEST_ACKNOWLEDGED";
    }
}
