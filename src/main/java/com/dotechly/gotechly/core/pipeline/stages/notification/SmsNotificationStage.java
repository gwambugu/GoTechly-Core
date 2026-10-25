package com.dotechly.gotechly.core.pipeline.stages.notification;

import com.dotechly.gotechly.core.pipeline.stages.callback.CallbackHttpClient;
import com.dotechly.gotechly.core.pipeline.stages.callback.CallbackTemplateRenderer;
import com.dotechly.gotechly.core.service.impl.TemplateLookupService;
import com.dotechly.gotechly.core.web.rest.payment.dto.ChannelResponseMapper;
import com.dotechly.gotechly.spi.service.AppConfigsLookup;
import com.dotechly.gotechly.spi.service.auth.OAuth2TokenProvider;
import org.springframework.stereotype.Component;

/**
 * {@code sms-notification} — sends the customer SMS for the transaction, rendered from the
 * {@code template_lookup} row named by {@code Details.additionalFields.smsTemplate} or, failing
 * that, {@code <paymentType>-SMS}.
 *
 * <p>See {@link AbstractNotificationStage} for the flow and the fire-and-forget contract.
 */
@Component
public class SmsNotificationStage extends AbstractNotificationStage {

    public SmsNotificationStage(TemplateLookupService templateLookupService,
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
        return NotificationChannel.SMS;
    }
}
