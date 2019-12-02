package com.blibli.oss.apiclient.client;

import com.blibli.oss.apiclient.annotation.ApiClient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@ApiClient(
  name = "hookbinClient",
  fallback = HookbinClientFallback.class
)
public interface HookbinClient {

  @RequestMapping(
    path = "/QJbWLMj9EOCZ7jrRNjyX",
    method = RequestMethod.POST
  )
  Mono<HookbinResponse> send(@RequestBody FirstModel model);

  @RequestMapping(
    path = "/VGOwBYJ8G2SX9Lm3gLZ6",
    method = RequestMethod.GET
  )
  Mono<HookbinResponse> send(@RequestParam("firstName") String firstName,
                             @RequestHeader("lastName") String lastName);

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class FirstModel {

    private String firstName;

    private String lastName;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class HookbinResponse {

    private boolean success;

  }

}