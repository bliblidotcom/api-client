package com.blibli.oss.apiclient;

import com.blibli.oss.apiclient.annotation.EnableApiClient;
import com.blibli.oss.apiclient.client.ExampleClient;
import com.blibli.oss.apiclient.client.HookbinClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@EnableApiClient
@SpringBootApplication
public class WebclientPluginApplication {

  public static void main(String[] args) {
    SpringApplication.run(WebclientPluginApplication.class, args);
  }

  @Component
  public class SampleCommandLineRunner implements CommandLineRunner {

    @Autowired
    private ExampleClient exampleClient;

    @Autowired
    private HookbinClient hookbinClient;

    @Override
    public void run(String... args) throws Exception {
      String response = exampleClient.get().block();
      System.out.println(response);

      HookbinClient.HookbinResponse hookbinResponse = hookbinClient.send(HookbinClient.FirstModel.builder()
        .firstName("Eko")
        .lastName("Khannedy")
        .build()).block();
      System.out.println(hookbinResponse);

      hookbinResponse = hookbinClient.send("Eko", "Khannedy").block();
      System.out.println(hookbinResponse);
    }
  }

  @RestController
  public class ClientController {

    @Autowired
    private ExampleClient exampleClient;

    @Autowired
    private HookbinClient hookbinClient;

    @GetMapping("/example")
    public Mono<String> example() {
      return exampleClient.get();
    }

    @GetMapping("/hook1")
    public Mono<HookbinClient.HookbinResponse> hook1() {
      return hookbinClient.send(HookbinClient.FirstModel.builder()
        .firstName("Eko")
        .lastName("Kurniawan")
        .build());
    }

    @GetMapping("/hook2")
    public Mono<HookbinClient.HookbinResponse> hook2() {
      return hookbinClient.send("Eko", "Khannedy");
    }

  }

}
