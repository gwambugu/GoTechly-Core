package com.dotechly.gotechly.core.pipeline.preprocess.outward.validation;


import com.dotechly.gotechly.core.pipeline.preprocess.outward.validation.steps.InputsFormatsValidationStep;
import com.dotechly.gotechly.core.pipeline.preprocess.outward.validation.steps.SignatureValidationStep;
import com.dotechly.gotechly.core.pipeline.preprocess.outward.validation.steps.TimestampValidationStep;
import com.dotechly.gotechly.core.service.impl.AppConfigsService;
import com.dotechly.gotechly.core.service.impl.ChannelsService;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.util.common.validation.ChainValidationService;
import com.dotechly.gotechly.spi.service.util.common.validation.ValidationResultSPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InputsValidationService implements ChainValidationService<PaymentLogSPI> {
    @Autowired
    ChannelsService channelsService;
    @Autowired
    AppConfigsService appConfigsService;

    @Override
    public ValidationResultSPI validate(PaymentLogSPI request, String direction) {
        return new InputsFormatsValidationStep()
                .linkWith(new TimestampValidationStep())
                .linkWith(new SignatureValidationStep(channelsService,appConfigsService, direction))
                .validate(request);
    }
}
