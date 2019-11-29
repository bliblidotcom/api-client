package com.blibli.oss.webclient;

import com.blibli.oss.webclient.annotation.EnableApiClient;
import com.blibli.oss.webclient.client.ExampleClient;
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

    @Override
    public void run(String... args) throws Exception {
      String response = exampleClient.get().block();
      System.out.println(response);
    }
  }

}
