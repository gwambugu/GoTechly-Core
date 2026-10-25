package com.dotechly.gotechly.core.service.auth;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.net.ssl.SSLException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Wiring for {@code OAuth2TokenProviderImpl}: a dedicated WebClient and a
 * single-thread scheduler for proactive token refresh. Both are intentionally
 * separate from {@code CallbackHttpClient} (payment-rate pool) and the
 * Gateway↔Core mTLS clients — token-fetch traffic is sparse, the upstream is
 * a public-CA HTTPS endpoint, and this client's lifecycle is owned by Core
 * (not by any plugin classloader).
 */
@Configuration
@EnableConfigurationProperties(OAuth2TokenProviderProperties.class)
public class OAuth2TokenProviderConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(OAuth2TokenProviderConfiguration.class);

    @Bean(name = "oauth2TokenWebClient")
    public WebClient oauth2TokenWebClient(OAuth2TokenProviderProperties props) {
        OAuth2TokenProviderProperties.Pool poolCfg = props.pool();

        ConnectionProvider provider = ConnectionProvider.builder("oauth2-token-pool")
                .maxConnections(poolCfg.maxConnections())
                .pendingAcquireMaxCount(poolCfg.pendingAcquireMaxCount())
                .pendingAcquireTimeout(poolCfg.pendingAcquireTimeout())
                .maxIdleTime(poolCfg.maxIdleTime())
                .maxLifeTime(poolCfg.maxLifeTime())
                .evictInBackground(poolCfg.evictInBackground())
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) props.connectTimeout().toMillis())
                .responseTimeout(props.responseTimeout())
                .doOnConnected(c -> c
                        .addHandlerLast(new ReadTimeoutHandler(props.readTimeout().toSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(props.writeTimeout().toSeconds(), TimeUnit.SECONDS)));

        if (props.trustAllCerts()) {
            try {
                SslContext sslContext = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
                httpClient = httpClient.secure(spec -> spec.sslContext(sslContext));
                LOG.warn("OAuth2 token WebClient: trust-all-certs=true — SSL validation DISABLED");
            } catch (SSLException e) {
                throw new RuntimeException("Failed to create trust-all SSL context for OAuth2 token client", e);
            }
        }

        LOG.info("Built OAuth2 token WebClient: maxConn={}, connectTimeout={}, responseTimeout={}, trustAll={}",
                poolCfg.maxConnections(), props.connectTimeout(), props.responseTimeout(), props.trustAllCerts());

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(64 * 1024))
                .build();
    }

    @Bean(name = "oauth2RefreshScheduler", destroyMethod = "shutdownNow")
    public ScheduledExecutorService oauth2RefreshScheduler(OAuth2TokenProviderProperties props) {
        return Executors.newScheduledThreadPool(
                props.refreshScheduler().corePoolSize(),
                r -> {
                    Thread t = new Thread(r, "oauth2-token-refresh");
                    t.setDaemon(true);
                    return t;
                });
    }
}
