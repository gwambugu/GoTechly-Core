package com.dotechly.gotechly.core.pipeline.stages.callback;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.CBS;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * Interprets the DFS core-banking answer to a {@code callback} POST.
 *
 * <p>Split out of {@link CallbackStage} so that stage stays what its name says: it resolves a
 * channel, renders a template, POSTs, and never halts. Reading a verdict out of the reply is
 * corridor-specific, and on this corridor the verdict can stop the pipeline — two different
 * contracts that do not belong in one stage.
 *
 * <p>Reads {@link CallbackStage#RESPONSE_ATTR} and clears it, so an answer is consumed exactly
 * once. Runs only for {@code Details.requestType = DFS-CORE.INWARD_PAYMENT}; every other channel
 * is a no-op, including one whose endpoint answers JSON — the XML parse sits inside the guard.
 *
 * <p>Persists onto {@code Details}:
 * <ul>
 *   <li>{@code responseCode} — DFS ResultCode (e.g. "0", "1003", "3015")</li>
 *   <li>{@code responseDesc} — DFS ResultDesc (human-readable)</li>
 *   <li>{@code status} — on a refusal with no CBS state, or on a confirmed posting</li>
 *   <li>additionalFields: {@code callback.responseCode}, {@code callback.responseDesc},
 *       {@code callback.status}, {@code dfsConversationId}, {@code dfsTransactionId},
 *       {@code dfsInnerResultCode}, {@code dfsInnerResultDesc}</li>
 * </ul>
 *
 * <p>A non-zero ResultCode halts {@code callback-rejected-transaction} — the same halt code
 * {@code callback} raised before the split, so every {@code compensation:} entry keyed on it is
 * unchanged. What does change is the halting stage name, which audit records as {@code actorId}.
 *
 * <p>Deliberately does not override {@code executeReactive}: the work is a small DOM parse with no
 * IO, so the interface default's {@code boundedElastic} offload is both correct and enough.
 */
@Component
public class DfsCallbackResponseStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(DfsCallbackResponseStage.class);

    /** The one corridor whose callback reply carries a verdict this stage understands. */
    private static final String DFS_INWARD_REQUEST_TYPE = "DFS-CORE.INWARD_PAYMENT";

    static final String HALT_CODE = "callback-rejected-transaction";

    @Override
    public String name() {
        return "dfs-callback-response";
    }

    @Override
    public int defaultOrder() {
        return 5100;   // immediately after callback (5000)
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        if (request.getDetails() == null) return request;

        String channel = request.getDetails().getRequestType();
        if (!DFS_INWARD_REQUEST_TYPE.equalsIgnoreCase(channel)) {
            LOG.info("[DFS-CB] for  {} {}", channel, request.getDetails().getRequestId());
            return request;
        }

        CallbackOutcome outcome = context.getAttribute(CallbackStage.RESPONSE_ATTR);
        // Consume it: an answer must never be read twice, and deriveCompensation copies whatever is
        // left here into every nested compensation context.
        context.setAttribute(CallbackStage.RESPONSE_ATTR, null);

        if (outcome == null || outcome.body() == null || outcome.body().isBlank()) {
            // callback either never ran in this stage list, or its POST failed — it swallows
            // transport errors by design. No answer is not a refusal, so carry on without halting.
            LOG.warn("[DFS-CB] No callback response recorded for requestId={} — nothing to interpret",
                    request.getDetails().getRequestId());
            return request;
        }

        try {
            Document doc = parseXml(outcome.body());
            CBS cbs = request.getCbs();

            String resultCode      = firstText(doc, "ResultCode");
            String resultDesc      = firstText(doc, "ResultDesc");
            String innerResultCode = firstText(doc, "InnerResultCode");
            String innerResultDesc = firstText(doc, "InnerResultDesc");
            String conversationId  = firstText(doc, "ConversationID");
            String transactionId   = firstText(doc, "TransactionID");
            boolean success = "0".equals(resultCode);
            request.getDetails().addAdditionalField("callback.responseCode",resultCode);
            request.getDetails().addAdditionalField("callback.responseDesc",resultDesc);
            request.getDetails().setResponseCode(resultCode);
            request.getDetails().setResponseDesc(resultDesc);
            if (conversationId  != null) request.getDetails().addAdditionalField("dfsConversationId",  conversationId);
            if (transactionId   != null) request.getDetails().addAdditionalField("dfsTransactionId",   transactionId);
            if (innerResultCode != null) request.getDetails().addAdditionalField("dfsInnerResultCode", innerResultCode);
            if (innerResultDesc != null) request.getDetails().addAdditionalField("dfsInnerResultDesc", innerResultDesc);

            if(!success){
                String reasonCode = "CB-"+resultCode;
                context.halt(name(), reasonCode, HALT_CODE, "Callback rejected transaction");
                String cbsStatus = (cbs!=null)? cbs.getStatus(): null;
                if(cbsStatus==null){
                    request.getDetails().setStatus("FAILED");
                    request.getDetails().setResponseCode(resultCode);
                    request.getDetails().setResponseDesc("Callback rejected transaction");
                }
                LOG.info("[DFS-CB] rejected transaction: status={}, resultCode={}, resultDesc={}, " +
                                "conversationId={}, transactionId={}",
                        request.getDetails().getStatus(), resultCode, resultDesc,
                        conversationId, transactionId);

            }else{
                String callbackStatus = success ? "SUCCESS" : "FAILED";
                request.getDetails().addAdditionalField("callback.status",callbackStatus);
                request.getDetails().setStatus(success ? "COMPLETED" : "FAILED");
//                String cbsStatus = (cbs!=null)? cbs.getStatus(): null;
//                if("POSTED".equalsIgnoreCase(cbsStatus) || "SUCCESS".equalsIgnoreCase(cbsStatus) ){
//                    String finalDesc = request.getDetails().getResponseDesc()+resultDesc;
//                    if(request.getDetails().getStatus()==null) request.getDetails().setStatus(success ? "COMPLETED" : "FAILED");
//                    request.getDetails().setResponseCode(resultCode);
//                    request.getDetails().setResponseDesc(finalDesc);
//                }
            }
            LOG.info("[DFS-CB] Details status={}, resultCode={}, resultDesc={}, " +
                            "conversationId={}, transactionId={}",
                    request.getDetails().getStatus(), resultCode, resultDesc,
                    conversationId, transactionId);

        } catch (Exception e) {
            LOG.warn("[DFS-CB] Could not parse callback response body to update details: {}", e.getMessage());
        }
        return request;
    }

    /**
     * Hardened DOM parse of an external callback response body.
     * <p>
     * The error handler is replaced deliberately: the default one writes
     * {@code [Fatal Error] ...} straight to {@code System.err}, bypassing logging entirely (and
     * {@code setErrorHandler(null)} restores that default rather than silencing it). Rethrowing
     * instead lets the caller's catch block report it.
     */
    private static Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new ErrorHandler() {
            @Override public void warning(SAXParseException e) { }
            @Override public void error(SAXParseException e) throws SAXException { throw e; }
            @Override public void fatalError(SAXParseException e) throws SAXException { throw e; }
        });
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    /** Returns the text content of the first element matching the given local name, or null. */
    private static String firstText(Document doc, String localName) {
        NodeList nodes = doc.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            nodes = doc.getElementsByTagName(localName);
        }
        if (nodes.getLength() > 0 && nodes.item(0).getFirstChild() != null) {
            return nodes.item(0).getFirstChild().getNodeValue();
        }
        return null;
    }
}
