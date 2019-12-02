package com.blibli.oss.apiclient;

import com.blibli.oss.apiclient.annotation.EnableApiClient;
import com.blibli.oss.apiclient.client.ExampleClient;
import com.blibli.oss.apiclient.client.HookbinClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

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

      HookbinClient.HookbinResponse hookbinResponse = hookbinClient.first(HookbinClient.FirstModel.builder()
        .firstName("Eko")
        .lastName("Khannedy")
        .build()).block();
      System.out.println(hookbinResponse);

      hookbinResponse = hookbinClient.second("Eko", "Khannedy").block();
      System.out.println(hookbinResponse);
    }
  }

}
