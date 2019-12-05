package com.blibli.oss.apiclient.client;

import com.blibli.oss.apiclient.client.model.FirstRequest;
import com.blibli.oss.apiclient.client.model.FirstResponse;
import com.blibli.oss.apiclient.client.model.SecondResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ExampleClientFallback implements ExampleClient {

  @Override
  public Mono<FirstResponse> first(FirstRequest request) {
    return Mono.just(FirstResponse.builder()
      .hello("Ups First")
      .build());
  }

  @Override
  public Mono<SecondResponse> second() {
    return Mono.just(SecondResponse.builder()
      .hello("Ups Second")
      .build());
  }
}
