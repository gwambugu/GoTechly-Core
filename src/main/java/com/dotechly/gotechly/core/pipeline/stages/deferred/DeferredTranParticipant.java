package com.dotechly.gotechly.core.pipeline.stages.deferred;

import com.dotechly.gotechly.core.GoTechlyCoreApplication;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineExecutor;
import com.dotechly.gotechly.core.pipeline.PipelineRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;

import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

public class DeferredTranParticipant implements TransactionParticipant {
    private final static Logger LOG = LoggerFactory.getLogger(DeferredTranParticipant.class);
    @Override
    public int prepare(long id, Serializable context) {
        LOG.info("prepare..");
        return PREPARED;
    }

    @Override
    public void commit(long id, Serializable context) {
        Context ctx = (Context) context;
        PaymentLogSPI paymentLogSPI = ctx.get("DEFERRED_FLOW_PAYMENT_REQUEST");
        String direction = ctx.get("DEFERRED_DIRECTION");
         PipelineRegistry pipelineRegistry = GoTechlyCoreApplication.applicationContext.getBean(PipelineRegistry.class);
         PipelineExecutor pipelineExecutor = GoTechlyCoreApplication.applicationContext.getBean(PipelineExecutor.class);
        LOG.info("In commit phase Transaction requestId {}",paymentLogSPI.getDetails().getRequestId());
        String paymentType = paymentLogSPI.getDetails().getPaymentType();
        String apiType = "in-payment-deferred-flow";
        if(paymentLogSPI.getDetails().getRequestType().equalsIgnoreCase("payment-reversal")){
            apiType = "in-reversal-deferred-flow";
        }

        PipelineContext contextContext = new PipelineContext(paymentType, direction, apiType);
        List<PipelineStage> stages = pipelineRegistry.getPipeline(direction, paymentType, apiType);
        LOG.info("Executing reactive {} {} payment-processing pipeline for paymentType '{}' with {} stages",
                direction,apiType, paymentType, stages.size());
        pipelineExecutor.executeReactive(stages, contextContext, paymentLogSPI).subscribe(
                result -> {
                    // Success
                    LOG.info("Pipeline completed: {}", result);
                },
                error -> {
                    // Error
                    LOG.error("Pipeline failed", error);
                }
        );;

    }

    @Override
    public void abort(long id, Serializable context) {
        TransactionParticipant.super.abort(id, context);
    }
}
