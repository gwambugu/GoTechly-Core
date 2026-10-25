package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.node;

import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.Stan;

import com.dotechly.gotechly.spi.service.iso.StanProvider;
import org.springframework.stereotype.Service;

/**
 * The bean form of {@link Stan}, so a plugin can be handed a DE11 source through the SPI.
 *
 * <p>An adapter rather than making {@code Stan} itself a component: {@code Stan} is a static holder
 * because its other callers are jPOS participants that Q2 builds reflectively, with no Spring DI and
 * no constructor of our choosing. Spring cannot instantiate it and should not try. This class is the
 * one place the two worlds meet.
 *
 * <p><b>It resolves per call, and that is the point.</b> Registering a {@code StanGenerator} instance
 * in {@code PluginLoader}'s shared-services map would freeze whichever ordinal happened to be
 * installed at registration time; a plugin would then keep drawing from a block this instance may
 * have lost - exactly the duplicate-STAN case the lease exists to remove. Delegating to
 * {@link Stan#next()} on every call means the plugin follows
 * {@link StanNodeLeaseService#heartbeat()} whenever it re-claims a different ordinal.
 *
 * <p>It also removes any startup-ordering constraint. {@code PluginLoader} and
 * {@code StanNodeLeaseService} both initialise at {@code @PostConstruct} with no defined order
 * between them, but nothing here reads the generator until a plugin actually asks for a STAN - by
 * which time Q2 has started and the lease is long since claimed.
 */
@Service
public class StanService implements StanProvider {

    @Override
    public String next() {
        return Stan.next();
    }
}
