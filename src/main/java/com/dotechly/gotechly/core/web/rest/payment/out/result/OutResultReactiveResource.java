package com.dotechly.gotechly.core.web.rest.payment.out.result;

import com.dotechly.gotechly.core.service.PaymentRendezvousImp;
import com.dotechly.gotechly.core.service.plugin.result.ReactiveResultProcessingService;
import com.dotechly.gotechly.spi.service.dto.payment.Details;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/payment/api/v1/out/reactive")
public class OutResultReactiveResource {

    private static final Logger LOG = LoggerFactory.getLogger(OutResultReactiveResource.class);


    private final ReactiveResultProcessingService reactiveResultProcessingService;
    private final PaymentRendezvousImp paymentRendezvous;

    public OutResultReactiveResource(
            ReactiveResultProcessingService reactiveResultProcessingService,
                                     PaymentRendezvousImp paymentRendezvous) {
        this.reactiveResultProcessingService = reactiveResultProcessingService;
        this.paymentRendezvous = paymentRendezvous;
    }
    @PostMapping("/result/{paymentType}")
    public Mono<String> result(@PathVariable("paymentType") String paymentType, @RequestBody String result) {
        PaymentLogSPI req = new PaymentLogSPI();
        Details details = new Details();
        details.setPaymentType(paymentType);
        req.setDetails(details);
        LOG.debug("Reactive result request: {}", result);
        return reactiveResultProcessingService.resultProcessingRequest(req, result, "outward");
    }
}
