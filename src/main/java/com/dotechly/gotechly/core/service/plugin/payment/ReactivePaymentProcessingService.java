package com.dotechly.gotechly.core.service.plugin.payment;

import com.dotechly.gotechly.core.config.outward.OutPaymentProcessingHandlerRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineExecutor;
import com.dotechly.gotechly.core.pipeline.PipelineRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.core.pipeline.preprocess.outward.validation.InputsValidationService;
import com.dotechly.gotechly.spi.service.CBSRequestBuilder;
import com.dotechly.gotechly.spi.service.dto.payment.Details;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.outward.OutPaymentRequestProcessingHandler;
import com.dotechly.gotechly.spi.service.util.common.validation.ValidationResultSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ReactivePaymentProcessingService {

    private static final Logger LOG = LoggerFactory.getLogger(ReactivePaymentProcessingService.class);

    private final PipelineRegistry pipelineRegistry;
    private final PipelineExecutor pipelineExecutor;
    private final InputsValidationService inputsValidationService;
    private final OutPaymentProcessingHandlerRegistry outPaymentRegistry;

    public ReactivePaymentProcessingService(PipelineRegistry pipelineRegistry,
                                            PipelineExecutor pipelineExecutor,
                                            InputsValidationService inputsValidationService,
                                            OutPaymentProcessingHandlerRegistry outPaymentRegistry) {
        this.pipelineRegistry = pipelineRegistry;
        this.pipelineExecutor = pipelineExecutor;
        this.inputsValidationService = inputsValidationService;
        this.outPaymentRegistry = outPaymentRegistry;
    }

    public Mono<PaymentLogSPI> paymentProcessingRequest(PaymentLogSPI paymentLogSPI, String direction) {
        LOG.info("Reactive payment-processing request: {}", paymentLogSPI);
        ValidationResultSPI validationResultSPI = inputsValidationService.validate(paymentLogSPI,direction);
        if(!validationResultSPI.isValid()){//Not Valid
            LOG.error("Validation of inputs failed. {} {}",validationResultSPI.getErrorCode(),validationResultSPI.getErrorMsg());
            Details details = paymentLogSPI.getDetails();
            details.setResponseCode(validationResultSPI.getErrorCode());
            details.setStatusResponseCode(validationResultSPI.getErrorMsg());
            paymentLogSPI.setDetails(details);
            return Mono.just(paymentLogSPI);
        }
        paymentLogSPI = buildCBSRequestIfPluginImplementsBuilder(paymentLogSPI,direction);
        String paymentType = paymentLogSPI.getDetails().getPaymentType();
        PipelineContext context = new PipelineContext(paymentType, direction, "payment-processing");
        List<PipelineStage> stages = pipelineRegistry.getPipeline(direction, paymentType, "payment-processing");

        LOG.debug("Executing reactive {} payment-processing pipeline for paymentType '{}' with {} stages",
                direction, paymentType, stages.size());

        return pipelineExecutor.executeReactive(stages, context, paymentLogSPI);
    }

    /**
     * Run a money-movement phase other than the default {@code payment-processing} — currently
     * {@code credit-reversal}, an inbound instruction to reverse a credit we already posted.
     *
     * <p>A reversal is money movement, so it belongs to this service rather than the status or
     * validation ones; it is a separate <em>phase</em> because it needs a different core-banking
     * operation and acts on an existing transaction, which a shared stage list cannot express
     * without every stage in it testing the message type for itself.
     *
     * @param apiType the phase whose stage list to resolve and run
     */
    public Mono<PaymentLogSPI> paymentProcessingRequest(PaymentLogSPI paymentLogSPI, String direction,
                                                        String apiType) {
        LOG.info("Reactive {} request: {}", apiType, paymentLogSPI);
        String paymentType = paymentLogSPI.getDetails().getPaymentType();
        PipelineContext context = new PipelineContext(paymentType, direction, apiType);
        List<PipelineStage> stages = pipelineRegistry.getPipeline(direction, paymentType, apiType);

        LOG.debug("Executing reactive {} {} pipeline for paymentType '{}' with {} stages",
                direction, apiType, paymentType, stages.size());

        return pipelineExecutor.executeReactive(stages, context, paymentLogSPI);
    }

    public String paymentProcessingRequestAsync(PaymentLogSPI paymentLogSPI, String direction) {
        LOG.info("Reactive payment-processing Async request: {}", paymentLogSPI);
        ValidationResultSPI validationResultSPI = inputsValidationService.validate(paymentLogSPI,direction);
        if (!validationResultSPI.isValid()) {
            LOG.error("Validation of inputs failed. {} {}",
                    validationResultSPI.getErrorCode(), validationResultSPI.getErrorMsg());
            return "{\"errorCode\":\"" + validationResultSPI.getErrorCode()
                    + "\",\"errorMessage\":\"" + validationResultSPI.getErrorMsg() + "\"}";
        }
        String paymentType = paymentLogSPI.getDetails().getPaymentType();
        PipelineContext context = new PipelineContext(paymentType, direction, "payment-processing");
        List<PipelineStage> stages = pipelineRegistry.getPipeline(direction, paymentType, "payment-processing");

        LOG.debug("Executing reactive {} payment-processing pipeline for paymentType '{}' with {} stages",
                direction, paymentType, stages.size());

         pipelineExecutor.executeReactive(stages, context, paymentLogSPI);
        String asyncResponse = (String) context.getAttribute("async-response");
        return asyncResponse != null ? asyncResponse : "REQUEST_ACKNOWLEDGED";
    }
    private PaymentLogSPI buildCBSRequestIfPluginImplementsBuilder(PaymentLogSPI paymentLogSPI,String direction){
        try {
            OutPaymentRequestProcessingHandler handler;
            handler = outPaymentRegistry.getHandler(paymentLogSPI.getDetails().getPaymentType());
            if (handler instanceof CBSRequestBuilder builder) {
                try {
                    paymentLogSPI = builder.buildISORequest(paymentLogSPI,direction);
                    return paymentLogSPI;
                } catch (IllegalArgumentException e) {
                    LOG.warn("{} CBSRequestBuilder payload failed to build request: {}", paymentLogSPI.getDetails().getPaymentType(), e.getMessage());
                }
            }
        } catch (IllegalArgumentException e) {
            LOG.error("IllegalArgumentException {}",e.getMessage());
        }
        return paymentLogSPI;
    }
}
