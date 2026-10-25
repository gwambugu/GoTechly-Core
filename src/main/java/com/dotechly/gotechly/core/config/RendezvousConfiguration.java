package com.dotechly.gotechly.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RendezvousProperties.class)
public class RendezvousConfiguration {
}
