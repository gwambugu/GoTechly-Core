package com.dotechly.gotechly.core.util.general;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gotechly.util.custom.httpclient")
public class CustomHttpClientProperties {

    private int poolMaxConnections = 100;
    private long poolMaxIdleTimeMs = 60_000;
    private long poolMaxLifeTimeMs = 300_000;
    private long poolPendingAcquireTimeoutMs = 45_000;

    private int connectionTimeoutMs = 5_000;
    private long readTimeoutMs = 30_000;
    private long writeTimeoutMs = 5_000;
    private long responseTimeoutMs = 30_000;

    private boolean trustAllCerts = false;

    // --- Optional mTLS properties (only needed for mutual TLS endpoints) ---
    private String mtlsKeystorePath;
    private String mtlsKeystorePassword;
    private String mtlsTruststorePath;
    private String mtlsTruststorePassword;
    private boolean mtlsTrustAllCerts = false;

    public int getPoolMaxConnections() { return poolMaxConnections; }
    public void setPoolMaxConnections(int poolMaxConnections) { this.poolMaxConnections = poolMaxConnections; }

    public long getPoolMaxIdleTimeMs() { return poolMaxIdleTimeMs; }
    public void setPoolMaxIdleTimeMs(long poolMaxIdleTimeMs) { this.poolMaxIdleTimeMs = poolMaxIdleTimeMs; }

    public long getPoolMaxLifeTimeMs() { return poolMaxLifeTimeMs; }
    public void setPoolMaxLifeTimeMs(long poolMaxLifeTimeMs) { this.poolMaxLifeTimeMs = poolMaxLifeTimeMs; }

    public long getPoolPendingAcquireTimeoutMs() { return poolPendingAcquireTimeoutMs; }
    public void setPoolPendingAcquireTimeoutMs(long poolPendingAcquireTimeoutMs) { this.poolPendingAcquireTimeoutMs = poolPendingAcquireTimeoutMs; }

    public int getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public void setConnectionTimeoutMs(int connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }

    public long getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(long readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    public long getWriteTimeoutMs() { return writeTimeoutMs; }
    public void setWriteTimeoutMs(long writeTimeoutMs) { this.writeTimeoutMs = writeTimeoutMs; }

    public long getResponseTimeoutMs() { return responseTimeoutMs; }
    public void setResponseTimeoutMs(long responseTimeoutMs) { this.responseTimeoutMs = responseTimeoutMs; }

    public boolean isTrustAllCerts() { return trustAllCerts; }
    public void setTrustAllCerts(boolean trustAllCerts) { this.trustAllCerts = trustAllCerts; }

    public String getMtlsKeystorePath() { return mtlsKeystorePath; }
    public void setMtlsKeystorePath(String mtlsKeystorePath) { this.mtlsKeystorePath = mtlsKeystorePath; }

    public String getMtlsKeystorePassword() { return mtlsKeystorePassword; }
    public void setMtlsKeystorePassword(String mtlsKeystorePassword) { this.mtlsKeystorePassword = mtlsKeystorePassword; }

    public String getMtlsTruststorePath() { return mtlsTruststorePath; }
    public void setMtlsTruststorePath(String mtlsTruststorePath) { this.mtlsTruststorePath = mtlsTruststorePath; }

    public String getMtlsTruststorePassword() { return mtlsTruststorePassword; }
    public void setMtlsTruststorePassword(String mtlsTruststorePassword) { this.mtlsTruststorePassword = mtlsTruststorePassword; }

    public boolean isMtlsTrustAllCerts() { return mtlsTrustAllCerts; }
    public void setMtlsTrustAllCerts(boolean mtlsTrustAllCerts) { this.mtlsTrustAllCerts = mtlsTrustAllCerts; }
}
