package com.blibli.oss.webclient.client;

import com.blibli.oss.webclient.annotation.ApiClient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@ApiClient(
  name = "hookbinClient"
)
public interface HookbinClient {

  @RequestMapping(
    path = "/QJbWLMj9EOCZ7jrRNjyX",
    method = RequestMethod.POST,
    produces = MediaType.APPLICATION_JSON_VALUE,
    consumes = MediaType.APPLICATION_JSON_VALUE
  )
  Mono<HookbinResponse> first(@RequestBody FirstModel model);

  @RequestMapping(
    path = "/VGOwBYJ8G2SX9Lm3gLZ6",
    method = RequestMethod.GET,
    produces = MediaType.APPLICATION_JSON_VALUE,
    consumes = MediaType.APPLICATION_JSON_VALUE
  )
  Mono<HookbinResponse> second(@RequestParam("firstName") String firstName,
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