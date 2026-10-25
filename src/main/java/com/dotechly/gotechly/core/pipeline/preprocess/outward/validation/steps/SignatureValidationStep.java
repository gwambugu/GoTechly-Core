package com.dotechly.gotechly.core.pipeline.preprocess.outward.validation.steps;

import com.dotechly.gotechly.core.service.impl.AppConfigsService;
import com.dotechly.gotechly.core.service.impl.ChannelsService;
import com.dotechly.gotechly.spi.service.dto.payment.Details;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.dto.payment.Receiver;
import com.dotechly.gotechly.spi.service.dto.payment.Sender;
import com.dotechly.gotechly.spi.service.util.common.SignatureUtil;
import com.dotechly.gotechly.spi.service.util.common.validation.ValidationResultSPI;
import com.dotechly.gotechly.spi.service.util.common.validation.ValidationStepSPI;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class SignatureValidationStep extends ValidationStepSPI<PaymentLogSPI> {
    private final Logger log = LoggerFactory.getLogger(SignatureValidationStep.class);
    private final ChannelsService channelsService;
    private final AppConfigsService appConfigsService;
    private final String direction;
    public SignatureValidationStep(ChannelsService channelsService,AppConfigsService appConfigsService,String direction){
        this.channelsService = channelsService;
        this.appConfigsService = appConfigsService;
        this.direction = direction;
    }
    @Override
    public ValidationResultSPI validate(PaymentLogSPI toValidate) {
        log.info("SignatureValidationStep");
        Boolean isVerified = false;
        if("inward".equals(direction) && toValidate.getDetails().getFreeText1()==null){
            return checkNext(toValidate, toValidate.getDetails().getPaymentType());
        }
        if(toValidate.getDetails().getFreeText1()==null || toValidate.getDetails().getFreeText1().isBlank()) {
             isVerified = SignatureUtil.verifySig(toValidate,channelsService,appConfigsService);
        }else{
             isVerified = SignatureUtil.verifySig(toValidate,channelsService);
        }
        if(isVerified){
            return checkNext(toValidate, toValidate.getDetails().getPaymentType());
        }else{
            toValidate.getDetails().setStatus("FAILED");
            return ValidationResultSPI.invalid("Invalid hash", "VL-06");
        }


    }

}
