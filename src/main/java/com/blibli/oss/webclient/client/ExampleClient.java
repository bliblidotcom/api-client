package com.blibli.oss.webclient.client;

import com.blibli.oss.webclient.annotation.ApiClient;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@ApiClient(
  name = "exampleClient",
  url = "http://example.com"
)
public interface ExampleClient {

  @RequestMapping("/")
  Mono<String> get();

}
