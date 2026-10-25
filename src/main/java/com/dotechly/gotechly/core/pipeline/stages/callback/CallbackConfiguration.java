package com.dotechly.gotechly.core.pipeline.stages.callback;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CallbackHttpClientProperties.class)
public class CallbackConfiguration {
}
