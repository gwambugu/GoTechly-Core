package com.dotechly.gotechly.core.pipeline.preprocess.outward.validation.steps;

import com.dotechly.gotechly.core.service.impl.AppConfigsService;
import com.dotechly.gotechly.core.service.impl.ChannelsService;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.util.common.SignatureUtil;
import com.dotechly.gotechly.spi.service.util.common.validation.ValidationResultSPI;
import com.dotechly.gotechly.spi.service.util.common.validation.ValidationStepSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class TimestampValidationStep extends ValidationStepSPI<PaymentLogSPI> {
    private final Logger log = LoggerFactory.getLogger(TimestampValidationStep.class);
    public TimestampValidationStep(){
    }
    @Override
    public ValidationResultSPI validate(PaymentLogSPI toValidate) {
        if (!isTimestampOK(toValidate.getDetails().getRequestTimestamp(), toValidate.getReceiver().getCountryCode())) {
            toValidate.getDetails().setStatus("FAILED");
            return ValidationResultSPI.invalid("Invalid Timestamp", "VL-07");
        }else{
            return checkNext(toValidate, toValidate.getDetails().getPaymentType());
        }


    }
    private  boolean isTimestampOK(Timestamp timestamp,String countryCode) {
        if (timestamp == null) {
            return false;
        }
        Clock kigaliClock = Clock.system(ZoneId.of("Africa/Nairobi"));
        if(countryCode.equalsIgnoreCase("RW")){
            kigaliClock = Clock.system(ZoneId.of("Africa/Kigali"));
        }

        Instant providedTime = timestamp.toInstant();
        Instant currentTime = Instant.now(kigaliClock);

        long differenceMillis = Math.abs(
                Duration.between(providedTime, currentTime).toMillis()
        );
        log.info("TimestampValidationStep current {} provided {} diff {}",currentTime,providedTime,differenceMillis );
        return differenceMillis < Duration.ofMinutes(5).toMillis();
    }

}
