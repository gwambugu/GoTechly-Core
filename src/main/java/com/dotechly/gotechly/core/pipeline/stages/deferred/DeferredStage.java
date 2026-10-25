package com.dotechly.gotechly.core.pipeline.stages.deferred;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.tm.bean.AppQBean;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.Context;
import org.jpos.util.LogEvent;
import org.jpos.util.NameRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class DeferredStage implements PipelineStage {
    private static final Logger LOG = LoggerFactory.getLogger(DeferredStage.class);

    @Override
    public String name() {
        return "deferred-flow";
    }

    @Override
    public int defaultOrder() {
        return 50;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        LOG.info("SpaceStage ");
         placePaymentRequestToSpace(request,"DEFERRED-FLOW_TXN_PAYMENT");
        return request;
    }

    public PaymentLogSPI placePaymentRequestToSpace(PaymentLogSPI paymentLogSPI,
                                                    String logEventDescription) {
        LOG.info("DEFERRED-FLOW Placing transaction payment to space: {}", paymentLogSPI);
        try {
            AppQBean appQBean = (AppQBean) NameRegistrar.get("appQBean");
            String spaceN = appQBean.getConfiguration().get("input-space");
            Long timeout = appQBean.getConfiguration().getLong("spaceTimeout");
            String realm = "DEFERRED_FLOW";
            LogEvent event = new LogEvent(realm, logEventDescription);
            Date now = new Date();
            // Thread-safe selection of queue
            String queueName =  AppQBean.nextAsyncFlowTransactionManagerQueue();
            LOG.info("DEFERRED_FLOW Placing requestId='{}' to queueName={}", paymentLogSPI.getDetails().getRequestId(), queueName);
            event.addMessage("Transaction ID: " + paymentLogSPI.getDetails().getRequestId());
            event.addMessage("Start Time: " + now);
            Context context = new Context();
            Space<String, Context> space = SpaceFactory.getSpace(spaceN);
            context.put("DEFERRED_FLOW_PAYMENT_REQUEST", paymentLogSPI);
            context.put("DEFERRED_DIRECTION", "inward");
            context.put("timestamp", now);
            // Place request into space
            space.out(queueName, context, timeout);
            return paymentLogSPI;
        } catch (NameRegistrar.NotFoundException e) {
            LOG.error("NameRegistrar.NotFoundException: {}", e.getMessage());
            return null;
        }
    }
}
