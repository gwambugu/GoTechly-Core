package com.dotechly.gotechly.core.pipeline.stages.notification;

import com.dotechly.gotechly.core.pipeline.stages.callback.CallbackHttpClient;
import com.dotechly.gotechly.core.pipeline.stages.callback.CallbackTemplateRenderer;
import com.dotechly.gotechly.core.service.impl.TemplateLookupService;
import com.dotechly.gotechly.core.web.rest.payment.dto.ChannelResponseMapper;
import com.dotechly.gotechly.spi.service.AppConfigsLookup;
import com.dotechly.gotechly.spi.service.auth.OAuth2TokenProvider;
import org.springframework.stereotype.Component;

/**
 * {@code email-notification} — sends the customer e-mail for the transaction, rendered from the
 * {@code template_lookup} row named by {@code Details.additionalFields.emailTemplate} or, failing
 * that, {@code <paymentType>-EMAIL}.
 *
 * <p>See {@link AbstractNotificationStage} for the flow and the fire-and-forget contract.
 */
@Component
public class EmailNotificationStage extends AbstractNotificationStage {

    public EmailNotificationStage(TemplateLookupService templateLookupService,
                                CallbackTemplateRenderer templateRenderer,
                                CallbackHttpClient httpClient,
                                ChannelResponseMapper channelResponseMapper,
                                OAuth2TokenProvider tokenProvider,
                                AppConfigsLookup appConfigsLookup) {
        super(templateLookupService, templateRenderer, httpClient, channelResponseMapper,
                tokenProvider, appConfigsLookup);
    }

    @Override
    protected NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }
}
