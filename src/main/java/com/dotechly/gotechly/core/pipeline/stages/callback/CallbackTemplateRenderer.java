package com.dotechly.gotechly.core.pipeline.stages.callback;


import com.dotechly.gotechly.core.web.rest.payment.dto.ChannelPaymentResponse;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders Freemarker templates stored in the Channels table using the channel-facing response as
 * the model — the same {@link ChannelPaymentResponse} the synchronous REST endpoints return, so a
 * channel sees one contract whether the outcome arrives as a reply or as a callback.
 *
 * Template variables available (all nested under {@code response}):
 *   ${response.requestId}, ${response.paymentType}, ${response.requestType}
 *   ${response.status}, ${response.responseCode}, ${response.responseDesc}, ${response.providerCode}
 *   ${response.amount}, ${response.currencyCode}, ${response.chargeAmount}, ${response.chargeCurrencyCode}
 *   ${response.gatewayRef}, ${response.providerRef}, ${response.responseTimestamp}
 *   ${response.resolved.accountName}, ${response.resolved.accountNumber}
 *   ${response.additionalFields["someKey"]}
 *
 * The model is built by serializing the record through Jackson, so template field names match the
 * JSON contract exactly and a field absent from the response is absent here too (rendered as an
 * empty string under classic-compatible mode).
 */
@Component
public class CallbackTemplateRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackTemplateRenderer.class);

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final Configuration freemarkerConfig;
    private final ObjectMapper objectMapper;
    private final Map<String, Template> templateCache = new ConcurrentHashMap<>();

    public CallbackTemplateRenderer() {
        this.objectMapper = new ObjectMapper();
        // Registers JavaTimeModule so Instant fields render rather than blowing up.
        this.objectMapper.findAndRegisterModules();
        this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_33);
        freemarkerConfig.setDefaultEncoding("UTF-8");
        // Classic compatible mode: nulls render as empty strings
        // rather than throwing exceptions — callbacks should be best-effort
        freemarkerConfig.setClassicCompatible(true);
    }

    /**
     * Render a Freemarker template string using the channel-facing response only.
     */
    public String render(String templateContent, ChannelPaymentResponse response) {
        return render(templateContent, response, null);
    }

    /**
     * Render a Freemarker template string with both the channel-facing response and the
     * full {@link PaymentLogSPI} sub-objects available as model variables.
     *
     * <p>Variables available in the template:
     * <ul>
     *   <li>{@code response.*} — {@link ChannelPaymentResponse} fields (requestId, amount, status, …)</li>
     *   <li>{@code sender.*} — {@code PaymentLogSPI.getSender()} fields (accountNumber, senderName, msisdn, …)</li>
     *   <li>{@code receiver.*} — {@code PaymentLogSPI.getReceiver()} fields (msisdn, accountNumber, currencyCode, …)</li>
     *   <li>{@code details.*} — {@code PaymentLogSPI.getDetails()} fields (amount, requestId, currencyCode, additionalFields, …)</li>
     *   <li>{@code cbs.*} — {@code PaymentLogSPI.getCbs()} fields (drAcc, crAcc, …)</li>
     * </ul>
     *
     * @param templateContent the Freemarker template string from Channels.callbackRequestTemplate
     * @param response        the channel-facing response for this transaction
     * @param paymentLog      the full pipeline payment log; may be null (falls back to response-only model)
     * @return rendered string (JSON, XML, or SOAP envelope depending on template)
     * @throws CallbackRenderException if template processing fails
     */
    public String render(String templateContent, ChannelPaymentResponse response, PaymentLogSPI paymentLog) {
        return render(templateContent, response, paymentLog, null);
    }

    /**
     * Render a Freemarker template with the full model plus any caller-supplied extra variables.
     * Extra variables (e.g. pre-encrypted credentials) are merged last and may override defaults.
     *
     * @param extraModel optional additional variables (e.g. {@code encryptedPassword},
     *                   {@code encryptedSecurityCredential}); may be null
     */
    public String render(String templateContent, ChannelPaymentResponse response,
                         PaymentLogSPI paymentLog, Map<String, Object> extraModel) {
        Map<String, Object> dataModel = buildDataModel(response);
        if (paymentLog != null) {
            if (paymentLog.getSender() != null) {
                dataModel.put("sender", objectMapper.convertValue(paymentLog.getSender(), MAP_TYPE));
            }
            if (paymentLog.getReceiver() != null) {
                dataModel.put("receiver", objectMapper.convertValue(paymentLog.getReceiver(), MAP_TYPE));
            }
            if (paymentLog.getDetails() != null) {
                dataModel.put("details", objectMapper.convertValue(paymentLog.getDetails(), MAP_TYPE));
            }
            if (paymentLog.getCbs() != null) {
                dataModel.put("cbs", objectMapper.convertValue(paymentLog.getCbs(), MAP_TYPE));
            }
        }
        if (extraModel != null) {
            dataModel.putAll(extraModel);
        }
        return processTemplate(templateContent, dataModel);
    }

    private Map<String, Object> buildDataModel(ChannelPaymentResponse response) {
        Map<String, Object> model = new HashMap<>();
        // Records expose x() accessors, not getX() — Freemarker's bean wrapper would not see them,
        // so the response goes through Jackson into a plain Map first.
        model.put("response", objectMapper.convertValue(response, MAP_TYPE));
        return model;
    }

    private String processTemplate(String templateContent, Map<String, Object> dataModel) {
        try {
            Template template = templateCache.computeIfAbsent(templateContent, content -> {
                try {
                    return new Template("callbackTemplate",
                            new StringReader(content), freemarkerConfig);
                } catch (IOException e) {
                    throw new CallbackRenderException(
                            "IO error parsing callback template: " + e.getMessage(), e);
                }
            });
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            String result = writer.toString();
            LOG.debug("Callback template rendered. Input length: {}, Output length: {}",
                    templateContent.length(), result.length());
            return result;
        } catch (IOException e) {
            throw new CallbackRenderException(
                    "IO error rendering callback template: " + e.getMessage(), e);
        } catch (TemplateException e) {
            throw new CallbackRenderException(
                    "Freemarker template error rendering callback: " + e.getMessage(), e);
        }
    }
}
