package com.dotechly.gotechly.core.pipeline.preprocess.outward.validation.steps;

import com.dotechly.gotechly.spi.service.dto.payment.Details;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.dto.payment.Receiver;
import com.dotechly.gotechly.spi.service.dto.payment.Sender;
import com.dotechly.gotechly.spi.service.util.common.validation.ValidationResultSPI;
import com.dotechly.gotechly.spi.service.util.common.validation.ValidationStepSPI;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class InputsFormatsValidationStep extends ValidationStepSPI<PaymentLogSPI> {
    private final Logger log = LoggerFactory.getLogger(InputsFormatsValidationStep.class);
    @Override
    public ValidationResultSPI validate(PaymentLogSPI toValidate) {
        log.info("InputsFormatsValidationStep");
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            final Validator validator = validatorFactory.getValidator();
            final Set<ConstraintViolation<Details>> detailsConstraintsViolations = validator.validate(toValidate.getDetails());
            if (!detailsConstraintsViolations.isEmpty()) {
                String errorMsg = "{";
                for (ConstraintViolation<?> error : detailsConstraintsViolations) {
                    errorMsg = errorMsg + " " + error.getPropertyPath() + ": " + error.getMessage() + ", ";
                }
                errorMsg = errorMsg.substring(0, errorMsg.lastIndexOf(",")) + " }";
                return ValidationResultSPI.invalid(errorMsg, "VL-01");
            }
            final Set<ConstraintViolation<Sender>> senderConstraintsViolations = validator.validate(toValidate.getSender());
            if (!senderConstraintsViolations.isEmpty()) {
                String errorMsg = "{";
                for (ConstraintViolation<?> error : senderConstraintsViolations) {
                    errorMsg = errorMsg + " " + error.getPropertyPath() + ": " + error.getMessage() + ", ";
                }
                errorMsg = errorMsg.substring(0, errorMsg.lastIndexOf(",")) + " }";
                return ValidationResultSPI.invalid(errorMsg, "VL-01");
            }
            final Set<ConstraintViolation<Receiver>> receiverConstraintsViolations = validator.validate(toValidate.getReceiver());
            if (!receiverConstraintsViolations.isEmpty()) {
                String errorMsg = "{";
                for (ConstraintViolation<?> error : receiverConstraintsViolations) {
                    errorMsg = errorMsg + " " + error.getPropertyPath() + ": " + error.getMessage() + ", ";
                }
                errorMsg = errorMsg.substring(0, errorMsg.lastIndexOf(",")) + " }";
                return ValidationResultSPI.invalid(errorMsg, "VL-01");
            }
        }
        return checkNext(toValidate, null);
    }

}
