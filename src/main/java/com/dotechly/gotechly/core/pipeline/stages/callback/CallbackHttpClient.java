package com.dotechly.gotechly.core.pipeline.stages.callback;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.net.ssl.SSLException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Reactive HTTP client for sending callbacks to source systems.
 * Supports JSON, XML, and SOAP content types.
 * Cloned/simplified from MpesaReactiveHttpClient — no mTLS, no baseUrl.
 */
@Component
public class CallbackHttpClient implements DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackHttpClient.class);

    private final WebClient webClient;
    private final ConnectionProvider connectionProvider;
    private final Duration defaultResponseTimeout;

    public CallbackHttpClient(CallbackHttpClientProperties props) {
        this.connectionProvider = ConnectionProvider.builder("callback-pool")
                .maxConnections(props.getPoolMaxConnections())
                .maxIdleTime(Duration.ofMillis(props.getPoolMaxIdleTimeMs()))
                .maxLifeTime(Duration.ofMillis(props.getPoolMaxLifeTimeMs()))
                .pendingAcquireTimeout(Duration.ofMillis(props.getPoolPendingAcquireTimeoutMs()))
                .pendingAcquireMaxCount(props.getPoolMaxPendingAcquire())
                .evictInBackground(Duration.ofSeconds(30))
                .metrics(true)
                .build();

        HttpClient httpClient = buildNettyClient(
                connectionProvider,
                props.getConnectionTimeoutMs(),
                props.getResponseTimeoutMs(),
                props.getReadTimeoutMs(),
                props.getWriteTimeoutMs(),
                props.isTrustAllCerts());

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .build();

        this.defaultResponseTimeout = Duration.ofMillis(props.getResponseTimeoutMs());

        LOG.info("CallbackHttpClient initialized - pool=[max={}], " +
                        "timeouts=[connect={}ms, read={}ms, write={}ms, response={}ms], trustAll={}",
                props.getPoolMaxConnections(), props.getConnectionTimeoutMs(),
                props.getReadTimeoutMs(), props.getWriteTimeoutMs(),
                props.getResponseTimeoutMs(), props.isTrustAllCerts());
    }

    /**
     * POST JSON body to the given absolute URL.
     */
    public Mono<ClientHttpResponse> postJson(String url, String body,
                                             Map<String, String> headers, Duration timeout) {
        LOG.info("[CALLBACK] >> POST {} | body: {}", url, body);
        return webClient.post()
                .uri(url)
                .headers(h -> { if (headers != null) h.setAll(headers); })
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(responseBody -> new ClientHttpResponse(
                                        response.statusCode().value(),
                                        responseBody,
                                        responseBody
                                ))
                )
                .doOnNext(resp -> LOG.info("[CALLBACK] << POST {} | status: {} | body: {}",
                        url, resp.statusCode(), resp.body()))
                .timeout(resolveTimeout(timeout))
                .onErrorResume(ex -> {
                    LOG.error("Callback POST JSON failed to {}: {}", url, ex.getMessage(), ex);
                    return Mono.just(new ClientHttpResponse(-3, ex.getMessage(), ex.getMessage()));
                });
    }

    /**
     * POST XML body to the given absolute URL.
     */
    public Mono<ClientHttpResponse> postXml(String url, String xmlBody,
                                            Map<String, String> headers, Duration timeout) {
        LOG.info("[CALLBACK] >> POST {} | body: {}", url, xmlBody);
        return webClient.post()
                .uri(url)
                .headers(h -> { if (headers != null) h.setAll(headers); })
                .contentType(MediaType.APPLICATION_XML)
                .accept(MediaType.APPLICATION_XML)
                .bodyValue(xmlBody)
                .exchangeToMono(response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(responseBody -> new ClientHttpResponse(
                                        response.statusCode().value(),
                                        responseBody,
                                        responseBody
                                ))
                )
                .doOnNext(resp -> LOG.info("[CALLBACK] << POST {} | status: {} | body: {}",
                        url, resp.statusCode(), resp.body()))
                .timeout(resolveTimeout(timeout))
                .onErrorResume(ex -> {
                    LOG.error("Callback POST XML failed to {}: {}", url, ex.getMessage(), ex);
                    return Mono.just(new ClientHttpResponse(-3, ex.getMessage(), ex.getMessage()));
                });
    }

    /**
     * POST SOAP envelope to the given absolute URL.
     * Sets Content-Type: text/xml; charset=UTF-8 and SOAPAction header.
     */
    public Mono<ClientHttpResponse> postSoap(String url, String soapBody,
                                             Map<String, String> headers, Duration timeout) {
        LOG.info("[CALLBACK] >> POST {} | body: {}", url, soapBody);
        MediaType textXmlUtf8 = new MediaType("text", "xml", StandardCharsets.UTF_8);

        return webClient.post()
                .uri(url)
                .headers(h -> {
                    if (headers != null) h.setAll(headers);
                    if (h.getFirst("SOAPAction") == null) h.set("SOAPAction", "\"\"");
                })
                .contentType(textXmlUtf8)
                .accept(MediaType.ALL)
                .bodyValue(soapBody)
                .exchangeToMono(response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(responseBody -> new ClientHttpResponse(
                                        response.statusCode().value(),
                                        responseBody,
                                        responseBody
                                ))
                )
                .doOnNext(resp -> LOG.info("[CALLBACK] << POST {} | status: {} | body: {}",
                        url, resp.statusCode(), resp.body()))
                .timeout(resolveTimeout(timeout))
                .onErrorResume(ex -> {
                    LOG.error("Callback POST SOAP failed to {}: {}", url, ex.getMessage(), ex);
                    return Mono.just(new ClientHttpResponse(-3, ex.getMessage(), ex.getMessage()));
                });
    }

    private Duration resolveTimeout(Duration timeout) {
        return timeout != null ? timeout : defaultResponseTimeout;
    }

    public record ClientHttpResponse(int statusCode, String rawBody, String body) {}

    @Override
    public void destroy() {
        connectionProvider.dispose();
        LOG.info("CallbackHttpClient connection pool disposed.");
    }

    private static HttpClient buildNettyClient(
            ConnectionProvider provider,
            int connectionTimeoutMs,
            long responseTimeoutMs,
            long readTimeoutMs,
            long writeTimeoutMs,
            boolean trustAllCerts) {

        HttpClient client = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeoutMs, TimeUnit.MILLISECONDS))
                );

        if (trustAllCerts) {
            try {
                SslContext sslContext = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
                client = client.secure(spec -> spec.sslContext(sslContext));
            } catch (SSLException e) {
                throw new RuntimeException("Failed to create trust-all SSL context", e);
            }
        }

        return client;
    }

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            LOG.debug("[CALLBACK] >> {} {} Headers: {}", request.method(), request.url(), request.headers());
            return Mono.just(request);
        });
    }

    private static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            LOG.debug("[CALLBACK] << Response-Code: {} Headers: {}",
                    response.statusCode().value(), response.headers().asHttpHeaders());
            return Mono.just(response);
        });
    }
}
