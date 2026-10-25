package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.participants.validate;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.tm.bean.AppQBean;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.CbsExchange;
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
public class AccountValidationStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(AccountValidationStage.class);

    @Override
    public String name() {
        return "cbc-acc-validation";
    }

    @Override
    public int defaultOrder() {
        return 50;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        LOG.debug("[CBS Finacle cbc] Account validation for paymentType '{}', requestId '{}'",
                context.getPaymentType(),
                request.getDetails() != null ? request.getDetails().getRequestId() : "unknown");
        if (request.getReceiver().getAccountNumber() == null) {
            String reasonCode = "CBC-114";
            context.halt(name(), reasonCode, "cbc-validation-acc-missing", "Missing Credit account");
        }
        request = placeAccValidationReqToSpace(request, "ACC_INQ", request.getSender().getCountryCode());
        String f39 = request.getCbs().getAdditionalFields().get("validation.response.DE39");
        if ("000".equals(f39)) {
            return request;
        } else {
            String reasonCode = f39 == null ? "CBC-911" : "CBC-" + f39;
            String haltCode = f39 == null ? "cbc-validation-acc-unverified" : "cbc-acc-validation-acc-invalid";
            String reason = f39 == null ? "Time out occured" : f39;
            context.halt(name(), reasonCode, haltCode, reason);
        }

        return request;
    }

    public PaymentLogSPI placeAccValidationReqToSpace(PaymentLogSPI paymentLogSPI,
                                                   String logEventDescription,
                                                   String countryCode) {
        LOG.info("Placing account validation to space: {}", paymentLogSPI);
        try {
            AppQBean appQBean = (AppQBean) NameRegistrar.get("appQBean");

            String spaceN = appQBean.getConfiguration().get("input-space");
            Long timeout = appQBean.getConfiguration().getLong("spaceTimeout");
            String realm = "INQ";
            LogEvent event = new LogEvent(realm, logEventDescription);
            Date now = new Date();

            // Thread-safe selection of queue
            String queueName = switch (countryCode) {
                case "KE" -> AppQBean.nextKeFinTransactionManagerQueue();
                case "UG" -> AppQBean.nextUgFinTransactionManagerQueue();
                case "RW" -> AppQBean.nextRwFinTransactionManagerQueue();
                case "TZ" -> AppQBean.nextTzFinTransactionManagerQueue();
                case "CD" -> AppQBean.nextCdFinTransactionManagerQueue();
                case "SS" -> AppQBean.nextSsFinTransactionManagerQueue();
                default -> throw new IllegalArgumentException("Invalid country code: " + countryCode);
            };

            LOG.info("Placing requestId='{}' to queueName={}", paymentLogSPI.getDetails().getRequestId(), queueName);
            event.addMessage("Transaction ID: " + paymentLogSPI.getDetails().getRequestId());
            event.addMessage("Start Time: " + now);

            Context context = new Context();
            Space<String, Context> space = SpaceFactory.getSpace(spaceN);

            CbsExchange.put(context, "CBC_ACC_VALIDATION_REQUEST", paymentLogSPI);
            context.put("CBC_MSG_TYPE", "CBC_ACCOUNT_VALIDATION");
            context.put("timestamp", now);

            // Place request into space
            space.out(queueName, context, timeout);

            // Wait for response (thread-safe)
            paymentLogSPI = CbsExchange.await(context, "CBC_ACC_VALIDATION_RESPONSE", 10000);
            return paymentLogSPI;
        } catch (NameRegistrar.NotFoundException e) {
            LOG.error("NameRegistrar.NotFoundException: {}", e.getMessage());
            return null;
        }
    }
}
