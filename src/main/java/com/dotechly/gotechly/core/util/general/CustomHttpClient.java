package com.dotechly.gotechly.core.util.general;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SimpleKeyManagerFactory;
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

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generic reactive HTTP client for GoTechly Core.
 * Supports JSON, XML, and SOAP content types with optional mutual TLS.
 * <p>
 * Three WebClient variants:
 * <ul>
 *   <li>{@code webClient} — standard HTTPS with certificate validation</li>
 *   <li>{@code insecureWebClient} — trusts all certificates (self-signed)</li>
 *   <li>{@code mtlsWebClient} — mutual TLS with client certificate (optional)</li>
 * </ul>
 */
@Component
public class CustomHttpClient implements DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(CustomHttpClient.class);
    private static final AtomicLong SOAP_MSG_ID = new AtomicLong(0);

    private final WebClient webClient;
    private final WebClient insecureWebClient;
    private final WebClient mtlsWebClient;
    private final ConnectionProvider connectionProvider;
    private final Duration defaultResponseTimeout;

    public CustomHttpClient(CustomHttpClientProperties props) {
        this.connectionProvider = ConnectionProvider.builder("custom-http-pool")
                .maxConnections(props.getPoolMaxConnections())
                .maxIdleTime(Duration.ofMillis(props.getPoolMaxIdleTimeMs()))
                .maxLifeTime(Duration.ofMillis(props.getPoolMaxLifeTimeMs()))
                .pendingAcquireTimeout(Duration.ofMillis(props.getPoolPendingAcquireTimeoutMs()))
                .evictInBackground(Duration.ofSeconds(30))
                .metrics(true)
                .build();

        this.defaultResponseTimeout = Duration.ofMillis(props.getResponseTimeoutMs());

        // --- Standard WebClient ---
        HttpClient httpClient = buildNettyClient(
                connectionProvider,
                props.getConnectionTimeoutMs(), props.getResponseTimeoutMs(),
                props.getReadTimeoutMs(), props.getWriteTimeoutMs(), false);

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .build();

        // --- Insecure (trust-all) WebClient ---
        HttpClient insecureHttpClient = buildNettyClient(
                connectionProvider,
                props.getConnectionTimeoutMs(), props.getResponseTimeoutMs(),
                props.getReadTimeoutMs(), props.getWriteTimeoutMs(), true);

        this.insecureWebClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(insecureHttpClient))
                .filter(logRequest())
                .filter(logResponse())
                .build();

        // --- mTLS WebClient (optional) ---
        String keystorePath = props.getMtlsKeystorePath();
        String truststorePath = props.getMtlsTruststorePath();

        if (keystorePath != null && truststorePath != null) {
            HttpClient mtlsHttpClient = buildMtlsNettyClient(
                    connectionProvider,
                    props.getConnectionTimeoutMs(), props.getResponseTimeoutMs(),
                    props.getReadTimeoutMs(), props.getWriteTimeoutMs(),
                    keystorePath, props.getMtlsKeystorePassword(),
                    truststorePath, props.getMtlsTruststorePassword(),
                    props.isMtlsTrustAllCerts());

            this.mtlsWebClient = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(mtlsHttpClient))
                    .filter(logRequest())
                    .filter(logResponse())
                    .build();
            LOG.info("mTLS WebClient initialized - keystore={}, truststore={}, trustAllCerts={}",
                    keystorePath, truststorePath, props.isMtlsTrustAllCerts());
        } else {
            this.mtlsWebClient = null;
            LOG.info("mTLS WebClient NOT configured (mtls-keystore-path / mtls-truststore-path not set)");
        }

        LOG.info("CustomHttpClient initialized - pool=[max={}], " +
                        "timeouts=[connect={}ms, read={}ms, write={}ms, response={}ms]",
                props.getPoolMaxConnections(), props.getConnectionTimeoutMs(),
                props.getReadTimeoutMs(), props.getWriteTimeoutMs(),
                props.getResponseTimeoutMs());
    }

    // ========================================================================================
    // POST — JSON
    // ========================================================================================

    public Mono<ClientHttpResponse> postJson(String url, String body,
                                             Map<String, String> headers, Duration timeout) {
        return doPost(webClient, url, body, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                headers, timeout, "POST JSON");
    }

    public Mono<ClientHttpResponse> postJsonTrustAll(String url, String body,
                                                     Map<String, String> headers, Duration timeout) {
        return doPost(insecureWebClient, url, body, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                headers, timeout, "POST JSON (trust-all)");
    }

    // ========================================================================================
    // POST — XML
    // ========================================================================================

    public Mono<ClientHttpResponse> postXml(String url, String xmlBody,
                                            Map<String, String> headers, Duration timeout) {
        return doPost(webClient, url, xmlBody, MediaType.APPLICATION_XML, MediaType.APPLICATION_XML,
                headers, timeout, "POST XML");
    }

    public Mono<ClientHttpResponse> postXmlTrustAll(String url, String xmlBody,
                                                    Map<String, String> headers, Duration timeout) {
        return doPost(insecureWebClient, url, xmlBody, MediaType.APPLICATION_XML, MediaType.APPLICATION_XML,
                headers, timeout, "POST XML (trust-all)");
    }

    // ========================================================================================
    // POST — SOAP
    // ========================================================================================

    public Mono<ClientHttpResponse> postSoap(String url, String soapBody,
                                             Map<String, String> headers, Duration timeout) {
        return doPostSoap(webClient, url, soapBody, headers, timeout, "POST SOAP");
    }

    public Mono<ClientHttpResponse> postSoapTrustAll(String url, String soapBody,
                                                     Map<String, String> headers, Duration timeout) {
        return doPostSoap(insecureWebClient, url, soapBody, headers, timeout, "POST SOAP (trust-all)");
    }

    /**
     * POST SOAP envelope over mutual TLS.
     * Returns error Mono if mTLS is not configured.
     */
    public Mono<ClientHttpResponse> postSoapMtls(String url, String soapBody,
                                                 Map<String, String> headers, Duration timeout) {
        if (mtlsWebClient == null) {
            return Mono.error(new IllegalStateException(
                    "mTLS WebClient not configured. Set mtls-keystore-path/mtls-truststore-path in properties."));
        }
        return doPostSoap(mtlsWebClient, url, soapBody, headers, timeout, "POST SOAP (mTLS)");
    }

    // ========================================================================================
    // GET
    // ========================================================================================

    public Mono<ClientHttpResponse> getAny(String url, Map<String, String> headers,
                                           Duration timeout, MediaType acceptType) {
        return doGet(webClient, url, headers, timeout, acceptType, "GET");
    }

    public Mono<ClientHttpResponse> getAnyTrustAll(String url, Map<String, String> headers,
                                                   Duration timeout, MediaType acceptType) {
        return doGet(insecureWebClient, url, headers, timeout, acceptType, "GET (trust-all)");
    }

    public Mono<ClientHttpResponse> getAny(URI uri, Map<String, String> headers,
                                           Duration timeout, MediaType acceptType) {
        return doGet(webClient, uri, headers, timeout, acceptType, "GET");
    }

    public Mono<ClientHttpResponse> getAnyTrustAll(URI uri, Map<String, String> headers,
                                                   Duration timeout, MediaType acceptType) {
        return doGet(insecureWebClient, uri, headers, timeout, acceptType, "GET (trust-all)");
    }

    // ========================================================================================
    // Accessors
    // ========================================================================================

    public WebClient getWebClient() { return webClient; }
    public WebClient getInsecureWebClient() { return insecureWebClient; }
    public WebClient getMtlsWebClient() { return mtlsWebClient; }

    public record ClientHttpResponse(int statusCode, String rawBody, String body) {}

    @Override
    public void destroy() {
        connectionProvider.dispose();
        LOG.info("CustomHttpClient connection pool disposed.");
    }

    // ========================================================================================
    // Internal — deduplicated POST / GET / SOAP
    // ========================================================================================

    private Mono<ClientHttpResponse> doPost(WebClient client, String url, String body,
                                            MediaType contentType, MediaType acceptType,
                                            Map<String, String> headers, Duration timeout,
                                            String label) {
        return client.post()
                .uri(url)
                .headers(h -> { if (headers != null) h.setAll(headers); })
                .contentType(contentType)
                .accept(acceptType)
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
                .timeout(resolveTimeout(timeout))
                .onErrorResume(ex -> {
                    Throwable mapped = mapConnectionError(ex, url);
                    LOG.error("{} failed to {}: {}", label, url, mapped.getMessage(), mapped);
                    return Mono.just(new ClientHttpResponse(-3, mapped.getMessage(), mapped.getMessage()));
                });
    }

    private Mono<ClientHttpResponse> doPostSoap(WebClient client, String url, String soapBody,
                                                Map<String, String> headers, Duration timeout,
                                                String label) {
        MediaType textXmlUtf8 = new MediaType("text", "xml", StandardCharsets.UTF_8);
        long msgId = SOAP_MSG_ID.incrementAndGet();

        LOG.info("""
                Outbound SOAP Message
                ---------------------------
                ID: {}
                Address: {}
                Http-Method: POST
                Content-Type: text/xml; charset=UTF-8
                Label: {}
                Payload: {}
                --------------------------------------""",
                msgId, url, label, soapBody);

        return client.post()
                .uri(url)
                .headers(h -> {
                    if (headers != null) h.setAll(headers);
                    if (h.getFirst("SOAPAction") == null) h.set("SOAPAction", "\"\"");
                })
                .contentType(textXmlUtf8)
                .accept(MediaType.ALL)
                .bodyValue(soapBody)
                .exchangeToMono(response -> {
                    int statusCode = response.statusCode().value();

                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(responseBody -> {
                                LOG.info("""
                                        Inbound SOAP Message
                                        ----------------------------
                                        ID: {}
                                        Response-Code: {}
                                        Content-Type: {}
                                        Headers: {}
                                        Payload: {}
                                        --------------------------------------""",
                                        msgId, statusCode,
                                        response.headers().asHttpHeaders().getContentType(),
                                        response.headers().asHttpHeaders(),
                                        responseBody);

                                return new ClientHttpResponse(statusCode, responseBody, responseBody);
                            });
                })
                .timeout(resolveTimeout(timeout))
                .onErrorResume(ex -> {
                    Throwable mapped = mapConnectionError(ex, url);
                    LOG.error("{} failed [ID={}] to {}: {}", label, msgId, url, mapped.getMessage(), mapped);
                    return Mono.just(new ClientHttpResponse(-3, mapped.getMessage(), mapped.getMessage()));
                });
    }

    private Mono<ClientHttpResponse> doGet(WebClient client, String url,
                                           Map<String, String> headers, Duration timeout,
                                           MediaType acceptType, String label) {
        return client.get()
                .uri(url)
                .headers(h -> { if (headers != null) h.setAll(headers); })
                .accept(acceptType)
                .exchangeToMono(response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(responseBody -> new ClientHttpResponse(
                                        response.statusCode().value(),
                                        responseBody,
                                        responseBody
                                ))
                )
                .timeout(resolveTimeout(timeout))
                .onErrorResume(ex -> {
                    Throwable mapped = mapConnectionError(ex, url);
                    LOG.error("{} failed to {}: {}", label, url, mapped.getMessage(), mapped);
                    return Mono.just(new ClientHttpResponse(-3, mapped.getMessage(), mapped.getMessage()));
                });
    }

    private Mono<ClientHttpResponse> doGet(WebClient client, URI uri,
                                           Map<String, String> headers, Duration timeout,
                                           MediaType acceptType, String label) {
        return client.get()
                .uri(uri)
                .headers(h -> { if (headers != null) h.setAll(headers); })
                .accept(acceptType)
                .exchangeToMono(response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(responseBody -> new ClientHttpResponse(
                                        response.statusCode().value(),
                                        responseBody,
                                        responseBody
                                ))
                )
                .timeout(resolveTimeout(timeout))
                .onErrorResume(ex -> {
                    Throwable mapped = mapConnectionError(ex, uri.toString());
                    LOG.error("{} failed to {}: {}", label, uri, mapped.getMessage(), mapped);
                    return Mono.just(new ClientHttpResponse(-3, mapped.getMessage(), mapped.getMessage()));
                });
    }

    private Duration resolveTimeout(Duration timeout) {
        return timeout != null ? timeout : defaultResponseTimeout;
    }

    // ========================================================================================
    // Categorized error mapping (ported from MpesaReactiveHttpClient)
    // ========================================================================================

    private Throwable mapConnectionError(Throwable ex, String url) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        if (root instanceof SSLHandshakeException || root instanceof SSLException) {
            return new RuntimeException("SSL/TLS error communicating with " + url + ": " + root.getMessage(), ex);
        }
        if (root instanceof io.netty.channel.ConnectTimeoutException) {
            return new RuntimeException("Connection timeout to " + url + ": " + root.getMessage(), ex);
        }
        if (root instanceof io.netty.handler.timeout.WriteTimeoutException) {
            return new RuntimeException("Write timeout sending request to " + url, ex);
        }
        if (root instanceof io.netty.handler.timeout.ReadTimeoutException) {
            return new RuntimeException("Read timeout waiting for response from " + url, ex);
        }
        if (root instanceof java.net.ConnectException) {
            return new RuntimeException("Connection refused by " + url + ": " + root.getMessage(), ex);
        }
        if (root instanceof io.netty.handler.timeout.TimeoutException) {
            return new RuntimeException("Response timeout from " + url + ": " + root.getMessage(), ex);
        }
        if (root.getClass().getSimpleName().equals("PoolAcquireTimeoutException")) {
            return new RuntimeException("Connection pool exhausted for " + url + ": " + root.getMessage(), ex);
        }

        return new RuntimeException("Unexpected error communicating with " + url + ": " + root.getMessage(), ex);
    }

    // ========================================================================================
    // Netty HttpClient builders
    // ========================================================================================

    private static HttpClient buildNettyClient(
            ConnectionProvider provider,
            int connectionTimeoutMs, long responseTimeoutMs,
            long readTimeoutMs, long writeTimeoutMs,
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

    private static HttpClient buildMtlsNettyClient(
            ConnectionProvider provider,
            int connectionTimeoutMs, long responseTimeoutMs,
            long readTimeoutMs, long writeTimeoutMs,
            String keystorePath, String keystorePassword,
            String truststorePath, String truststorePassword,
            boolean trustAllCerts) {

        try {
            // Load client keystore — auto-detect type (PKCS12 vs JKS)
            KeyStore keyStore = loadKeyStore(keystorePath, keystorePassword);

            // Extract private key and cert chain
            String keyAlias = findKeyAlias(keyStore);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keystorePassword.toCharArray());
            java.security.cert.Certificate[] chain = keyStore.getCertificateChain(keyAlias);
            X509Certificate[] x509Chain = Arrays.stream(chain)
                    .map(c -> (X509Certificate) c)
                    .toArray(X509Certificate[]::new);

            LOG.info("mTLS client cert loaded: alias='{}', subject='{}', issuer='{}', chainLength={}",
                    keyAlias, x509Chain[0].getSubjectX500Principal(),
                    x509Chain[0].getIssuerX500Principal(), x509Chain.length);

            // Custom KeyManager that ALWAYS presents the client cert,
            // bypassing server's acceptable-CA list filtering
            X509ExtendedKeyManager forceKeyManager = new ForceClientCertKeyManager(
                    keyAlias, privateKey, x509Chain);

            // Wrap in SimpleKeyManagerFactory for proper Netty SslContextBuilder integration
            SimpleKeyManagerFactory forceKmf = new SimpleKeyManagerFactory() {
                @Override
                protected void engineInit(KeyStore ks, char[] password) {}
                @Override
                protected void engineInit(ManagerFactoryParameters params) {}
                @Override
                protected KeyManager[] engineGetKeyManagers() {
                    return new KeyManager[]{forceKeyManager};
                }
            };
            forceKmf.init((KeyStore) null, null);

            // Build trust manager factory
            TrustManagerFactory trustFactory;
            if (trustAllCerts) {
                LOG.warn("mTLS client using InsecureTrustManagerFactory — DO NOT use in production!");
                trustFactory = InsecureTrustManagerFactory.INSTANCE;
            } else {
                KeyStore trustStore = loadKeyStore(truststorePath, truststorePassword);
                trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustFactory.init(trustStore);
            }

            SslContext sslContext = SslContextBuilder.forClient()
                    .sslProvider(SslProvider.JDK)
                    .keyManager(forceKmf)
                    .trustManager(trustFactory)
                    .protocols("TLSv1.2")
                    .build();

            return HttpClient.create(provider)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMs)
                    .responseTimeout(Duration.ofMillis(responseTimeoutMs))
                    .doOnConnected(conn -> {
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS));
                        conn.addHandlerLast(new WriteTimeoutHandler(writeTimeoutMs, TimeUnit.MILLISECONDS));

                        // mTLS diagnostic logging — confirms client cert was presented
                        io.netty.handler.ssl.SslHandler sslHandler =
                                conn.channel().pipeline().get(io.netty.handler.ssl.SslHandler.class);
                        if (sslHandler != null) {
                            sslHandler.handshakeFuture().addListener(future -> {
                                if (future.isSuccess()) {
                                    SSLSession session = sslHandler.engine().getSession();
                                    java.security.cert.Certificate[] localCerts = session.getLocalCertificates();
                                    LOG.info("mTLS handshake OK: protocol={}, cipher={}, clientCertsSent={}",
                                            session.getProtocol(), session.getCipherSuite(),
                                            localCerts != null ? localCerts.length : 0);
                                    if (localCerts != null && localCerts.length > 0
                                            && localCerts[0] instanceof X509Certificate x509) {
                                        LOG.info("mTLS client cert presented: subject='{}', issuer='{}'",
                                                x509.getSubjectX500Principal(), x509.getIssuerX500Principal());
                                    } else {
                                        LOG.warn("mTLS handshake completed but NO client certificate was sent");
                                    }
                                } else {
                                    LOG.error("mTLS handshake FAILED: {}", future.cause().getMessage(), future.cause());
                                }
                            });
                        }
                    })
                    .secure(spec -> spec.sslContext(sslContext));

        } catch (KeyStoreException | IOException | NoSuchAlgorithmException |
                 CertificateException | UnrecoverableKeyException e) {
            throw new RuntimeException("Failed to create mTLS Netty HttpClient - " +
                    "keystore=" + keystorePath + ", truststore=" + truststorePath, e);
        }
    }

    // ========================================================================================
    // Keystore utilities
    // ========================================================================================

    private static KeyStore loadKeyStore(String path, String password) throws IOException,
            KeyStoreException, NoSuchAlgorithmException, CertificateException {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(path)) {
                ks.load(fis, password.toCharArray());
            }
            LOG.info("Loaded keystore '{}' as PKCS12 ({} entries)", path, ks.size());
            return ks;
        } catch (IOException e) {
            LOG.debug("Keystore '{}' is not PKCS12, trying JKS", path);
        }
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(path)) {
            ks.load(fis, password.toCharArray());
        }
        LOG.info("Loaded keystore '{}' as JKS ({} entries)", path, ks.size());
        return ks;
    }

    private static String findKeyAlias(KeyStore keyStore) throws KeyStoreException {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                return alias;
            }
        }
        throw new KeyStoreException("Keystore contains no private key entries");
    }

    // ========================================================================================
    // Logging filters
    // ========================================================================================

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            LOG.debug(">> {} {} Headers: {}", request.method(), request.url(), request.headers());
            return Mono.just(request);
        });
    }

    private static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            LOG.debug("<< Response-Code: {} Headers: {}",
                    response.statusCode().value(), response.headers().asHttpHeaders());
            return Mono.just(response);
        });
    }

    // ========================================================================================
    // ForceClientCertKeyManager — always presents client cert regardless of server CA list
    // ========================================================================================

    private static class ForceClientCertKeyManager extends X509ExtendedKeyManager {
        private final String alias;
        private final PrivateKey key;
        private final X509Certificate[] chain;

        ForceClientCertKeyManager(String alias, PrivateKey key, X509Certificate[] chain) {
            this.alias = alias;
            this.key = key;
            this.chain = chain;
        }

        @Override
        public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
            LOG.info(">>> TLS ENGINE REQUESTED CLIENT CERT: keyTypes={}, issuers={}, returning alias='{}'",
                    Arrays.toString(keyType),
                    issuers != null ? Arrays.toString(issuers) : "ANY (no CA restriction)",
                    alias);
            return alias;
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            LOG.info(">>> TLS SOCKET REQUESTED CLIENT CERT: keyTypes={}, issuers={}, returning alias='{}'",
                    Arrays.toString(keyType),
                    issuers != null ? Arrays.toString(issuers) : "ANY",
                    alias);
            return alias;
        }

        @Override public X509Certificate[] getCertificateChain(String alias) { return chain; }
        @Override public PrivateKey getPrivateKey(String alias) { return key; }
        @Override public String[] getClientAliases(String keyType, Principal[] issuers) { return new String[]{alias}; }

        // --- Server-side methods (unused for client mTLS) ---
        @Override public String[] getServerAliases(String keyType, Principal[] issuers) { return null; }
        @Override public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) { return null; }
        @Override public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) { return null; }
    }
}
