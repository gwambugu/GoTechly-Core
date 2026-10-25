package com.dotechly.gotechly.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gotechly.rendezvous")
public class RendezvousProperties {

    private String keyPrefix = "xp:result:";
    private int waitTimeoutSeconds = 3;
    private int keyTtlSeconds = 30;

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public int getWaitTimeoutSeconds() { return waitTimeoutSeconds; }
    public void setWaitTimeoutSeconds(int waitTimeoutSeconds) { this.waitTimeoutSeconds = waitTimeoutSeconds; }

    public int getKeyTtlSeconds() { return keyTtlSeconds; }
    public void setKeyTtlSeconds(int keyTtlSeconds) { this.keyTtlSeconds = keyTtlSeconds; }
}
