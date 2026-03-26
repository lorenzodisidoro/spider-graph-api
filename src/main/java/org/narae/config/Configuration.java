package org.narae.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

@org.springframework.context.annotation.Configuration
@EnableConfigurationProperties(ApiSecurityProperties.class)
public class Configuration {
}
