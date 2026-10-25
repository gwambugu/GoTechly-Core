package com.dotechly.gotechly.core.pipeline.stages.callback;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gotechly.callback")
public class CallbackHttpClientProperties {

    private int poolMaxConnections = 500;
    private long poolMaxIdleTimeMs = 30_000;
    private long poolMaxLifeTimeMs = 300_000;
    private long poolPendingAcquireTimeoutMs = 45_000;
    private int poolMaxPendingAcquire = 1000;

    private int connectionTimeoutMs = 5_000;
    private long readTimeoutMs = 10_000;
    private long writeTimeoutMs = 5_000;
    private long responseTimeoutMs = 10_000;

    private boolean trustAllCerts = false;

    public int getPoolMaxConnections() { return poolMaxConnections; }
    public void setPoolMaxConnections(int poolMaxConnections) { this.poolMaxConnections = poolMaxConnections; }

    public long getPoolMaxIdleTimeMs() { return poolMaxIdleTimeMs; }
    public void setPoolMaxIdleTimeMs(long poolMaxIdleTimeMs) { this.poolMaxIdleTimeMs = poolMaxIdleTimeMs; }

    public long getPoolMaxLifeTimeMs() { return poolMaxLifeTimeMs; }
    public void setPoolMaxLifeTimeMs(long poolMaxLifeTimeMs) { this.poolMaxLifeTimeMs = poolMaxLifeTimeMs; }

    public long getPoolPendingAcquireTimeoutMs() { return poolPendingAcquireTimeoutMs; }
    public void setPoolPendingAcquireTimeoutMs(long poolPendingAcquireTimeoutMs) { this.poolPendingAcquireTimeoutMs = poolPendingAcquireTimeoutMs; }

    public int getPoolMaxPendingAcquire() { return poolMaxPendingAcquire; }
    public void setPoolMaxPendingAcquire(int poolMaxPendingAcquire) { this.poolMaxPendingAcquire = poolMaxPendingAcquire; }

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
}
