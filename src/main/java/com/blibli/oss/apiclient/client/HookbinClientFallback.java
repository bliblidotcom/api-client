package com.blibli.oss.apiclient.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class HookbinClientFallback implements HookbinClient {

  @Override
  public Mono<HookbinResponse> first(FirstModel model) {
    log.info("Receive fallback");
    return Mono.just(HookbinResponse.builder()
      .success(false)
      .build());
  }

  @Override
  public Mono<HookbinResponse> second(String firstName, String lastName) {
    log.info("Receive fallback");
    return Mono.just(HookbinResponse.builder()
      .success(false)
      .build());
  }
}
