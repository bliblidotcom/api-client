package com.blibli.oss.apiclient.client;

import com.blibli.oss.apiclient.annotation.ApiClient;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@ApiClient(
  name = "exampleClient"
)
public interface ExampleClient {

  @RequestMapping("/")
  Mono<String> get();

}
