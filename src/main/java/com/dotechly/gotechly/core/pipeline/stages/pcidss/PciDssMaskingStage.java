package com.dotechly.gotechly.core.pipeline.stages.pcidss;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * PCI-DSS compliance stage.
 * Runs BEFORE the plugin handler to ensure sensitive cardholder data
 * is masked/tokenized before being sent to external payment providers.
 *
 * TODO: Implement PAN masking, tokenization, field-level encryption.
 *
 * This stage should:
 * <ul>
 *   <li>Mask card numbers (show only last 4 digits in logs)</li>
 *   <li>Tokenize sensitive fields before they reach plugin handlers</li>
 *   <li>Ensure no sensitive data leaks into log output</li>
 * </ul>
 *
 * Use {@link PipelineContext#setAttribute} to store original values
 * if a post-plugin stage needs to de-tokenize.
 */
@Component
public class PciDssMaskingStage implements PipelineStage  {

    private static final Logger LOG = LoggerFactory.getLogger(PciDssMaskingStage.class);

    @Override
    public String name() {
        return "pci-dss-masking";
    }

    @Override
    public int defaultOrder() {
        return 200;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        LOG.debug("[PCI-DSS] Masking sensitive data for paymentType '{}' -- NO-OP placeholder",
                context.getPaymentType());

        // Placeholder — pass through unchanged
        return request;
    }
}
