package com.dotechly.gotechly.core.util.general;

import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
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

/**
 * Renders Freemarker templates stored in the Channels table
 * using PaymentLogSPI data as the model.
 *
 * Template variables available:
 *   ${sender.msisdn}, ${sender.accountNumber}, ${sender.senderName}, ...
 *   ${receiver.msisdn}, ${receiver.accountNumber}, ${receiver.receiverName}, ...
 *   ${details.requestId}, ${details.status}, ${details.responseCode}, ${details.amount}, ...
 *   ${cbs.tranRef}, ${cbs.respCode}, ${cbs.respDesc}, ...
 *   ${psp.responseCode}, ${psp.responseDesc}, ${psp.resultCode}, ${psp.referenceCode}, ...
 */
@Component
public class CustomTemplateRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(CustomTemplateRenderer.class);

    private final Configuration freemarkerConfig;

    public CustomTemplateRenderer() {
        this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_33);
        freemarkerConfig.setDefaultEncoding("UTF-8");
        // Classic compatible mode: nulls render as empty strings
        // rather than throwing exceptions — callbacks should be best-effort
        freemarkerConfig.setClassicCompatible(true);
    }

    /**
     * Render a Freemarker template string using data from PaymentLogSPI.
     *
     * @param templateContent the Freemarker template string from Channels.callbackRequestTemplate
     * @param paymentLog      the PaymentLogSPI with payment data
     * @return rendered string (JSON, XML, or SOAP envelope depending on template)
     * @throws CustomRenderException if template processing fails
     */
    public String render(String templateContent, PaymentLogSPI paymentLog) {
        Map<String, Object> dataModel = buildDataModel(paymentLog);
        return processTemplate(templateContent, dataModel);
    }

    private Map<String, Object> buildDataModel(PaymentLogSPI paymentLog) {
        Map<String, Object> model = new HashMap<>();
        model.put("sender", paymentLog.getSender());
        model.put("receiver", paymentLog.getReceiver());
        model.put("details", paymentLog.getDetails());
        model.put("cbs", paymentLog.getCbs());
        model.put("psp", paymentLog.getPsp());
        return model;
    }

    private String processTemplate(String templateContent, Map<String, Object> dataModel) {
        try {
            Template template = new Template("generalTemplate",
                    new StringReader(templateContent), freemarkerConfig);
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);
            String result = writer.toString();
            LOG.debug("General template rendered. Input length: {}, Output length: {}",
                    templateContent.length(), result.length());
            return result;
        } catch (IOException e) {
            throw new CustomRenderException(
                    "IO error rendering callback template: " + e.getMessage(), e);
        } catch (TemplateException e) {
            throw new CustomRenderException(
                    "Freemarker template error rendering callback: " + e.getMessage(), e);
        }
    }
}
