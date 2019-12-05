package com.blibli.oss.apiclient;

import com.blibli.oss.apiclient.client.ExampleClient;
import com.blibli.oss.apiclient.client.model.FirstRequest;
import com.blibli.oss.apiclient.client.model.FirstResponse;
import com.blibli.oss.apiclient.client.model.SecondResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
public class ExampleClientTest {

  public static final FirstRequest FIRST_REQUEST = FirstRequest.builder()
    .name("Eko Kurniawan").build();
  public static final FirstResponse FIRST_RESPONSE = FirstResponse.builder()
    .hello("Hello Eko Kurniawan")
    .build();
  public static final SecondResponse SECOND_RESPONSE = SecondResponse.builder().hello("Hello").build();
  private static WireMockServer wireMockServer;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ExampleClient exampleClient;

  @BeforeAll
  static void beforeAll() {
    wireMockServer = new WireMockServer(8089);
    wireMockServer.start();
  }

  @Test
  void testFirst() throws JsonProcessingException {
    wireMockServer.stubFor(
      post(urlPathEqualTo("/first"))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON_VALUE))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
        .withRequestBody(equalTo(objectMapper.writeValueAsString(FIRST_REQUEST)))
        .willReturn(
          aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody(objectMapper.writeValueAsString(FIRST_RESPONSE))
        )
    );

    FirstResponse response = exampleClient.first(FIRST_REQUEST).block();
    assertEquals(FIRST_RESPONSE, response);
  }

  @Test
  void testSecond() throws JsonProcessingException {
    wireMockServer.stubFor(
      get(urlPathEqualTo("/second"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON_VALUE))
        .willReturn(
          aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody(objectMapper.writeValueAsString(SECOND_RESPONSE))
        )
    );

    SecondResponse response = exampleClient.second().block();
    assertEquals(SECOND_RESPONSE, response);
  }

  @AfterAll
  static void afterAll() {
    wireMockServer.stop();
  }
}
