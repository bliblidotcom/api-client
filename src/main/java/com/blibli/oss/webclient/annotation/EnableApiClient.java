package com.blibli.oss.webclient.annotation;

import com.blibli.oss.webclient.configuration.ApiClientConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ApiClientConfiguration.class)
public @interface EnableApiClient {

	String[] basePackages() default {};

}
