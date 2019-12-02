package com.blibli.oss.apiclient.configuration;

import com.blibli.oss.apiclient.properties.ApiClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
  ApiClientProperties.class
})
public class ApiClientConfiguration {
}
