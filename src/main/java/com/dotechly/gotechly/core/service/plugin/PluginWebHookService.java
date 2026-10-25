package com.dotechly.gotechly.core.service.plugin;

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
 * Handles plugin webhook callbacks where the PaymentLogSPI has already been
 * enriched with PSP response data by the plugin's jPOS participant.
 * <p>
 * Runs the result-processing pipeline stages (callback to source system, audit, etc.)
 * but skips the {@code out-result-handler} stage since the PaymentLogSPI is already complete.
 */
@Service
public class PluginWebHookService {

    private static final Logger LOG = LoggerFactory.getLogger(PluginWebHookService.class);

    private final PipelineRegistry pipelineRegistry;
    private final PipelineExecutor pipelineExecutor;

    public PluginWebHookService(PipelineRegistry pipelineRegistry, PipelineExecutor pipelineExecutor) {
        this.pipelineRegistry = pipelineRegistry;
        this.pipelineExecutor = pipelineExecutor;
    }

    public Mono<PaymentLogSPI> processWebhookResult(PaymentLogSPI paymentLogSPI, String direction) {
        String paymentType = paymentLogSPI.getDetails().getPaymentType();
        LOG.info("Plugin webhook response for paymentType='{}', requestId='{}', status='{}'",
                paymentType,
                paymentLogSPI.getDetails().getRequestId(),
                paymentLogSPI.getDetails().getStatus());

        PipelineContext context = new PipelineContext(paymentType, direction, "plugin-response-processing");

        List<PipelineStage> stages = pipelineRegistry.getPipeline(direction, paymentType, "plugin-response-processing");

        // Filter out the result-handler stage — PaymentLogSPI is already enriched by the plugin.
        // Remaining stages (callback, audit-via-executor, etc.) still run normally.
        List<PipelineStage> filteredStages = stages.stream()
                .filter(s -> !s.name().equals("out-response-handler") && !s.name().equals("in-response-handler"))
                .toList();

        LOG.debug("Executing webhook response pipeline for '{}' with {} stages (filtered from {})",
                paymentType, filteredStages.size(), stages.size());

        return pipelineExecutor.executeReactive(filteredStages, context, paymentLogSPI);
    }
}
