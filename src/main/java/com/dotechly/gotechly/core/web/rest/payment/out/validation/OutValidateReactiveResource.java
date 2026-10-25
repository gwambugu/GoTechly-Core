package com.dotechly.gotechly.core.web.rest.payment.out.validation;

import com.dotechly.gotechly.core.service.PaymentRendezvousImp;
import com.dotechly.gotechly.core.service.plugin.validation.ReactiveValidationService;
import com.dotechly.gotechly.core.web.rest.payment.dto.ChannelPaymentResponse;
import com.dotechly.gotechly.core.web.rest.payment.dto.ChannelResponseMapper;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/payment/api/v1/out/reactive")
public class OutValidateReactiveResource {

    private static final Logger LOG = LoggerFactory.getLogger(OutValidateReactiveResource.class);
    private static final String OUT_VALIDATION = ChannelResponseMapper.scopeFor("outward", "validation");
    private final ReactiveValidationService reactiveValidationService;
    private final PaymentRendezvousImp paymentRendezvous;
    private final ChannelResponseMapper channelResponseMapper;

    public OutValidateReactiveResource(ReactiveValidationService reactiveValidationService,
                                       PaymentRendezvousImp paymentRendezvous,
                                       ChannelResponseMapper channelResponseMapper) {
        this.reactiveValidationService = reactiveValidationService;
        this.paymentRendezvous = paymentRendezvous;
        this.channelResponseMapper = channelResponseMapper;
    }

    @PostMapping("/validate")
    public Mono<ChannelPaymentResponse> validate(@RequestBody PaymentLogSPI req) {
        LOG.debug("Reactive validate request: {}", req);
        return reactiveValidationService.validateRequest(req, "outward")
                .map(result -> channelResponseMapper.toResponse(result, OUT_VALIDATION));
    }
    @PostMapping("/sync-over-async-validate")
    public Mono<ChannelPaymentResponse> syncOverAsyncValidate(@RequestBody PaymentLogSPI req) {
        LOG.debug("Reactive syncOverAsyncValidate request: {}", req);
        return reactiveValidationService.validateRequest(req, "outward")
                .then(Mono.defer(() -> {
                    LOG.info("Sync over async waiting for result: {}", req);
                    return paymentRendezvous.park(req.getDetails().getRequestId());
                }))
                .map(result -> channelResponseMapper.toResponse(result, OUT_VALIDATION));
    }
    @PostMapping("/async-validate")
    public String validateAsync(@RequestBody PaymentLogSPI req) {
        LOG.debug("Async validate request: {}", req);
        return reactiveValidationService.validateRequestAsync(req, "outward");
    }

}
