package com.dotechly.gotechly.core.util.general;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CustomHttpClientProperties.class)
public class CustomHttpClientConfiguration {
}
