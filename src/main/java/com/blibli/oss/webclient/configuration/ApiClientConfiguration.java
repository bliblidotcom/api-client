package com.blibli.oss.webclient.configuration;

import com.blibli.oss.webclient.properties.ApiClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
  ApiClientProperties.class
})
public class ApiClientConfiguration {
}
