package com.dotechly.gotechly.core.web.rest.payment.out;

import com.dotechly.gotechly.core.service.plugin.validation.ReactiveValidationService;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment/api/v1/b2c/async")
public class OutAsyncResource {

    private static final Logger LOG = LoggerFactory.getLogger(OutAsyncResource.class);

    private final ReactiveValidationService reactiveValidationService;

    public OutAsyncResource(ReactiveValidationService reactiveValidationService) {
        this.reactiveValidationService = reactiveValidationService;
    }

    @PostMapping("/validate")
    public String validate(@RequestBody PaymentLogSPI req) {
        LOG.debug("Async validate request: {}", req);
        return reactiveValidationService.validateRequestAsync(req, "outward");
    }
}
