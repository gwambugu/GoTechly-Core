package com.dotechly.gotechly.core.web.rest.payment.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for the channel-facing response.
 *
 * <p>{@code additional-fields} is a per-payment-type <em>allowlist</em> of keys to lift out of
 * {@code Details.additionalFields} into {@link ChannelPaymentResponse#additionalFields()}. Nothing
 * is copied unless it is named here, so adding a corridor-specific field to the response is a
 * config change rather than a code change — without turning the response back into a passthrough
 * of the pipeline's internal scratch map.
 *
 * <pre>
 * gotechly:
 *   channel-response:
 *     additional-fields:
 *       RNDPS:
 *         - msgId
 *       MPESA:
 *         - ConversationID
 * </pre>
 *
 * <p>Keys are matched case-sensitively against {@code Details.additionalFields}. A configured key
 * that is absent from a given transaction is simply skipped.
 */
@Component
@ConfigurationProperties(prefix = "gotechly.channel-response")
public class ChannelResponseProperties {

    /** paymentType → keys allowed through from {@code Details.additionalFields}. */
    private Map<String, List<String>> additionalFields = new LinkedHashMap<>();

    public Map<String, List<String>> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(Map<String, List<String>> additionalFields) {
        this.additionalFields = (additionalFields == null) ? new LinkedHashMap<>() : additionalFields;
    }

    /** Allowed keys for a payment type; empty when nothing is configured. */
    public List<String> allowedKeysFor(String paymentType) {
        if (paymentType == null) {
            return List.of();
        }
        return additionalFields.getOrDefault(paymentType, List.of());
    }
}
