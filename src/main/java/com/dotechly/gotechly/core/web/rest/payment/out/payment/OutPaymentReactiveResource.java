package com.dotechly.gotechly.core.web.rest.payment.out.payment;

import com.dotechly.gotechly.core.service.PaymentRendezvousImp;
import com.dotechly.gotechly.core.service.plugin.payment.ReactivePaymentProcessingService;
import com.dotechly.gotechly.core.web.rest.payment.dto.ChannelPaymentResponse;
import com.dotechly.gotechly.core.web.rest.payment.dto.ChannelResponseMapper;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/payment/api/v1/out/reactive")
public class OutPaymentReactiveResource {

    private static final Logger LOG = LoggerFactory.getLogger(OutPaymentReactiveResource.class);
    private static final String OUT_PAYMENT = ChannelResponseMapper.scopeFor("outward", "payment-processing");

    private final ReactivePaymentProcessingService reactivePaymentProcessingService;
    private final PaymentRendezvousImp paymentRendezvous;
    private final ChannelResponseMapper channelResponseMapper;

    public OutPaymentReactiveResource( ReactivePaymentProcessingService reactivePaymentProcessingService,
                                      PaymentRendezvousImp paymentRendezvous,
                                       ChannelResponseMapper channelResponseMapper) {
        this.reactivePaymentProcessingService = reactivePaymentProcessingService;
        this.paymentRendezvous = paymentRendezvous;
        this.channelResponseMapper = channelResponseMapper;
    }

    @PostMapping("/payment")
    public Mono<ChannelPaymentResponse> payment(@RequestBody PaymentLogSPI req) {
        LOG.debug("Reactive payment request: {}", req);
        return reactivePaymentProcessingService.paymentProcessingRequest(req, "outward")
                .map(result -> channelResponseMapper.toResponse(result, OUT_PAYMENT));
    }

    @PostMapping("/sync-over-async-payment")
    public Mono<ChannelPaymentResponse>  syncOverAsyncValidate(@RequestBody PaymentLogSPI req) {
        LOG.debug("Reactive syncOverAsyncValidate request: {}", req);
        return reactivePaymentProcessingService.paymentProcessingRequest(req, "outward")
                .then(Mono.defer(() -> {
                    LOG.info("Sync over async waiting for result: {}", req);
                    return paymentRendezvous.park(req.getDetails().getRequestId());
                })).map(result -> channelResponseMapper.toResponse(result, OUT_PAYMENT));
    }

    @PostMapping("/async-payment")
    public String paymentAsync(@RequestBody PaymentLogSPI req) {
        LOG.debug("Async payment request: {}", req);
        return reactivePaymentProcessingService.paymentProcessingRequestAsync(req, "outward");
    }


}
