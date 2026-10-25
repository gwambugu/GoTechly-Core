package com.dotechly.gotechly.core.recovery;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds {@code gotechly.recovery.*}. */
@Configuration
@EnableConfigurationProperties(RecoveryProperties.class)
public class RecoveryConfiguration {
}
