package com.dotechly.gotechly.core.web.rest.payment.out.status;

import com.dotechly.gotechly.core.service.PaymentRendezvousImp;
import com.dotechly.gotechly.core.service.plugin.status.ReactiveStatusService;
import com.dotechly.gotechly.core.web.rest.payment.dto.ChannelPaymentResponse;
import com.dotechly.gotechly.core.web.rest.payment.dto.ChannelResponseMapper;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/payment/api/v1/out/reactive")
public class OutStatusReactiveResource {
    private static final String OUT_STATUS = ChannelResponseMapper.scopeFor("outward", "status-processing");
    private static final Logger LOG = LoggerFactory.getLogger(OutStatusReactiveResource.class);


    private final ReactiveStatusService reactiveStatusService;
    private final PaymentRendezvousImp paymentRendezvous;
    private final ChannelResponseMapper channelResponseMapper;

    public OutStatusReactiveResource(
                                     ReactiveStatusService reactiveStatusService,
                                     PaymentRendezvousImp paymentRendezvous,
                                     ChannelResponseMapper channelResponseMapper) {
        this.reactiveStatusService = reactiveStatusService;
        this.paymentRendezvous = paymentRendezvous;
        this.channelResponseMapper = channelResponseMapper;
    }

    @PostMapping("/status")
    public Mono<ChannelPaymentResponse>   status(@RequestBody PaymentLogSPI req) {
        LOG.debug("Reactive status request: {}", req);
        return reactiveStatusService.statusProcessingRequest(req, "outward")
                .map(result -> channelResponseMapper.toResponse(result, OUT_STATUS));
    }
}
