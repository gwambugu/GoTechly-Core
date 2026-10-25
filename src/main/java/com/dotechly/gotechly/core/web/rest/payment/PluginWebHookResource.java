package com.dotechly.gotechly.core.web.rest.payment;

import com.dotechly.gotechly.core.service.plugin.PluginWebHookService;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Webhook endpoint for plugins to post back enriched PaymentLogSPI after
 * asynchronous processing (e.g., jPOS TransactionManager completes payment).
 * <p>
 * The plugin has already updated the PSP object with provider response data.
 * This endpoint runs the remaining result-processing pipeline stages (callback
 * to source system, audit, etc.) to complete the transaction lifecycle.
 */
@RestController
@RequestMapping("/payment/api/v1/plugin/webhook")
public class PluginWebHookResource {

    private static final Logger LOG = LoggerFactory.getLogger(PluginWebHookResource.class);

    private final PluginWebHookService pluginWebHookService;

    public PluginWebHookResource(PluginWebHookService pluginWebHookService) {
        this.pluginWebHookService = pluginWebHookService;
    }

    @PostMapping("/response")
    public Mono<PaymentLogSPI> result(@RequestBody PaymentLogSPI paymentLogSPI) {
        LOG.info("Plugin webhook response received: requestId='{}', paymentType='{}', status='{}'",
                paymentLogSPI.getDetails() != null ? paymentLogSPI.getDetails().getRequestId() : "null",
                paymentLogSPI.getDetails() != null ? paymentLogSPI.getDetails().getPaymentType() : "null",
                paymentLogSPI.getDetails() != null ? paymentLogSPI.getDetails().getStatus() : "null");

        String direction = "outward";
        return pluginWebHookService.processWebhookResult(paymentLogSPI, direction);
    }
}
