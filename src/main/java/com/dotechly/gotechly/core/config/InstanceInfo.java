package com.dotechly.gotechly.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class InstanceInfo {

    private final String instanceId;
    private final String hostname;

    public InstanceInfo(@Value("${server.port:8081}") int port) {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "unknown";
        }
        this.hostname = host;
        this.instanceId = host + ":" + port;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getHostname() {
        return hostname;
    }
}
