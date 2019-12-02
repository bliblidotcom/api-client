package com.blibli.oss.apiclient.aop;

import com.blibli.oss.apiclient.annotation.ApiClient;
import com.blibli.oss.apiclient.interceptor.ApiClientInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ApiClientMethodInterceptor implements MethodInterceptor, InitializingBean, ApplicationContextAware {

  @Setter
  private ApplicationContext applicationContext;

  @Setter
  private Class<?> type;

  @Setter
  private String name;

  private WebClient webClient;

  private Object fallback;

  private RequestMappingMetadata metadata;

  @Override
  public void afterPropertiesSet() throws Exception {
    prepareAttribute();
    prepareWebClient();
    prepareFallback();
  }

  private void prepareAttribute() {
    metadata = new RequestMappingMetadataBuilder(applicationContext, type, name)
      .build();
  }

  private void prepareFallback() {
    ApiClient annotation = type.getAnnotation(ApiClient.class);
    if (annotation.fallback() != Void.class) {
      fallback = applicationContext.getBean(annotation.fallback());
    }
  }

  private void prepareWebClient() {
    ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);

    ExchangeStrategies strategies = ExchangeStrategies.builder().codecs(clientDefaultCodecsConfigurer -> {
      clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
      clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
    }).build();

    TcpClient tcpClient = TcpClient.create()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) metadata.getProperties().getConnectTimeout().toMillis())
      .doOnConnected(connection -> connection
        .addHandlerLast(new ReadTimeoutHandler(metadata.getProperties().getReadTimeout().toMillis(), TimeUnit.MILLISECONDS))
        .addHandlerLast(new WriteTimeoutHandler(metadata.getProperties().getWriteTimeout().toMillis(), TimeUnit.MILLISECONDS))
      );

    webClient = WebClient.builder()
      .exchangeStrategies(strategies)
      .baseUrl(metadata.getProperties().getUrl())
      .clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient)))
      .defaultHeaders(httpHeaders -> metadata.getProperties().getHeaders().forEach(httpHeaders::add))
      .filters(exchangeFilterFunctions ->
        metadata.getProperties().getInterceptors().forEach(interceptorClass ->
          exchangeFilterFunctions.add((ApiClientInterceptor) applicationContext.getBean(interceptorClass))
        )
      )
      .build();
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    return Mono.fromCallable(() -> webClient)
      .map(client -> doMethod(invocation))
      .map(client -> client.uri(uriBuilder -> getUri(uriBuilder, invocation)))
      .map(client -> doHeader(client, invocation))
      .map(client -> doBody(client, invocation))
      .flatMap(client -> client.retrieve().bodyToMono(metadata.getResponseBodyClasses().get(invocation.getMethod().getName())))
      .onErrorResume(throwable -> doFallback((Throwable) throwable, invocation));
  }

  private WebClient.RequestHeadersUriSpec<?> doMethod(MethodInvocation invocation) {
    RequestMethod method = metadata.getRequestMethods().get(invocation.getMethod().getName());
    if (method.equals(RequestMethod.GET)) {
      return webClient.get();
    } else if (method.equals(RequestMethod.POST)) {
      return webClient.post();
    } else if (method.equals(RequestMethod.PUT)) {
      return webClient.put();
    } else if (method.equals(RequestMethod.PATCH)) {
      return webClient.patch();
    } else if (method.equals(RequestMethod.DELETE)) {
      return webClient.delete();
    } else if (method.equals(RequestMethod.OPTIONS)) {
      return webClient.options();
    } else if (method.equals(RequestMethod.HEAD)) {
      return webClient.head();
    } else {
      return webClient.get();
    }
  }

  private URI getUri(UriBuilder builder, MethodInvocation invocation) {
    builder.path(metadata.getPaths().get(invocation.getMethod().getName()));

    metadata.getQueryParamPositions().get(invocation.getMethod().getName()).forEach((paramName, position) -> {
      builder.queryParam(paramName, invocation.getArguments()[position]);
    });

    Map<String, Object> uriVariables = new HashMap<>();
    metadata.getPathVariablePositions().get(invocation.getMethod().getName()).forEach((paramName, position) -> {
      uriVariables.put(paramName, invocation.getArguments()[position]);
    });

    return builder.build(uriVariables);
  }

  private WebClient.RequestHeadersSpec<?> doHeader(WebClient.RequestHeadersSpec<?> spec, MethodInvocation invocation) {
    metadata.getHeaders().get(invocation.getMethod().getName()).forEach((key, values) -> {
      spec.headers(httpHeaders -> httpHeaders.addAll(key, values));
    });

    metadata.getHeaderParamPositions().get(invocation.getMethod().getName()).forEach((key, position) -> {
      spec.headers(httpHeaders -> httpHeaders.add(key, String.valueOf(invocation.getArguments()[position])));
    });

    metadata.getCookieParamPositions().get(invocation.getMethod().getName()).forEach((key, position) -> {
      spec.cookies(cookies -> cookies.add(key, String.valueOf(invocation.getArguments()[position])));
    });

    return spec;
  }

  private WebClient.RequestHeadersSpec<?> doBody(WebClient.RequestHeadersSpec<?> client, MethodInvocation invocation) {
    if (client instanceof WebClient.RequestBodySpec) {
      Integer bodyPosition = metadata.getRequestBodyPositions().get(invocation.getMethod().getName());
      WebClient.RequestBodySpec bodySpec = (WebClient.RequestBodySpec) client;
      if (bodyPosition != null) {
        Object body = invocation.getArguments()[bodyPosition];
        return bodySpec.body(Mono.fromCallable(() -> body), body.getClass());
      }
    }
    return client;
  }

  private Mono doFallback(Throwable throwable, MethodInvocation invocation) {
    if (Objects.nonNull(fallback)) {
      return (Mono) ReflectionUtils.invokeMethod(invocation.getMethod(), fallback, invocation.getArguments());
    } else {
      return Mono.error(throwable);
    }
  }
}
