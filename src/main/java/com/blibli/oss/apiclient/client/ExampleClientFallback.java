package com.blibli.oss.apiclient.client;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ExampleClientFallback implements ExampleClient {

  @Override
  public Mono<String> get() {
    return Mono.fromCallable(() -> "Ups");
  }
}
