package com.blibli.oss.apiclient.client;

import com.blibli.oss.apiclient.annotation.ApiClient;
import com.blibli.oss.apiclient.client.model.FirstRequest;
import com.blibli.oss.apiclient.client.model.FirstResponse;
import com.blibli.oss.apiclient.client.model.SecondResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import reactor.core.publisher.Mono;

@ApiClient(
  name = "exampleClient",
  fallback = ExampleClientFallback.class
)
public interface ExampleClient {

  @RequestMapping(
    method = RequestMethod.POST,
    path = "/first",
    produces = MediaType.APPLICATION_JSON_VALUE,
    consumes = MediaType.APPLICATION_JSON_VALUE
  )
  Mono<FirstResponse> first(@RequestBody FirstRequest request);

  @RequestMapping(
    method = RequestMethod.GET,
    path = "/second",
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  Mono<SecondResponse> second();

}
