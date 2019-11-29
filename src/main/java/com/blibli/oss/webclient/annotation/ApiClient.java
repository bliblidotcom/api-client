package com.blibli.oss.webclient.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiClient {

  String name();

  String fallback() default "";

  String url() default "";

  boolean primary() default true;

}
