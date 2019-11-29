package com.blibli.oss.webclient.bean;

import com.blibli.oss.webclient.interceptor.ApiClientInterceptor;
import com.blibli.oss.webclient.properties.ApiClientProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ApiClientMethodInterceptor implements MethodInterceptor, InitializingBean, ApplicationContextAware {

  @Setter
  private ApplicationContext applicationContext;

  private WebClient webClient;

  @Setter
  private Class<?> type;

  @Setter
  private String url;

  @Setter
  private ApiClientProperties.ApiClientConfigProperties properties;

  @Setter
  private ApiClientProperties.ApiClientConfigProperties defaultProperties;

  @Setter
  private ObjectMapper objectMapper;

  @Override
  public void afterPropertiesSet() throws Exception {
    ExchangeStrategies strategies = ExchangeStrategies.builder().codecs(clientDefaultCodecsConfigurer -> {
      clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
      clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
    }).build();

    WebClient.Builder builder = WebClient.builder().exchangeStrategies(strategies).baseUrl(url);
    builder = configureProperties(builder, defaultProperties);
    builder = configureProperties(builder, properties);
    webClient = builder.build();
  }

  private WebClient.Builder configureProperties(WebClient.Builder builder,
                                                ApiClientProperties.ApiClientConfigProperties configProperties) {
    return Optional.ofNullable(configProperties)
      .map(properties -> doConfigureProperties(builder, properties))
      .orElse(builder);
  }

  private WebClient.Builder doConfigureProperties(WebClient.Builder builder,
                                                  ApiClientProperties.ApiClientConfigProperties properties) {
    return builder
      .defaultHeaders(httpHeaders ->
        properties.getHeaders().forEach(httpHeaders::add)
      )
      .filters(exchangeFilterFunctions ->
        properties.getInterceptors().forEach(interceptorClass ->
          exchangeFilterFunctions.add((ApiClientInterceptor) applicationContext.getBean(interceptorClass))
        )
      );
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Method method = invocation.getMethod();
    Parameter[] parameters = method.getParameters();
    Object[] arguments = invocation.getArguments();
    RequestMapping mapping = method.getAnnotation(RequestMapping.class);

    // webClient.post().uri("").header("", "").body();

    return Mono.<Object>fromCallable(() -> webClient)
      .map(client -> doMethod(mapping))
      .map(client -> client.uri(uriBuilder -> getUri(uriBuilder, mapping, parameters, arguments)))
      .map(client -> doHeader(mapping, client))
      .map(client -> {
        if (client instanceof WebClient.RequestBodySpec) {
          return doBody((WebClient.RequestBodySpec) client, parameters, arguments);
        } else {
          return client;
        }
      });

//		return Mono.<Object>fromCallable(() -> "Hello World")
//			.onErrorResume(throwable -> Mono.create(sink -> {
//				try {
//					sink.success(invocation.proceed());
//				} catch (Throwable e) {
//					sink.error(e);
//				}
//			}));
  }

  private WebClient.RequestHeadersSpec<?> doBody(WebClient.RequestBodySpec client, Parameter[] parameters, Object[] objects) {
    WebClient.RequestBodySpec bodySpec = client;

    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
      Object value = objects[i];

      if (requestBody != null && value != null) {
        return bodySpec.body(Mono.fromCallable(() -> value), value.getClass());
      }
    }

    return bodySpec;
  }

  private WebClient.RequestHeadersSpec<?> doHeader(RequestMapping mapping, WebClient.RequestHeadersSpec<?> spec) {
    return spec.headers(httpHeaders -> {
      for (String header : mapping.headers()) {
        String[] split = header.split("=");
        if (split.length > 1) {
          httpHeaders.add(split[0], split[1]);
        } else {
          httpHeaders.add(split[0], "");
        }
      }
      httpHeaders.addAll(HttpHeaders.CONTENT_TYPE, Arrays.asList(mapping.consumes()));
      httpHeaders.addAll(HttpHeaders.ACCEPT, Arrays.asList(mapping.produces()));
    });
  }

  private URI getUri(UriBuilder builder, RequestMapping mapping, Parameter[] parameters, Object[] objects) {
    builder.path(getPath(mapping));

    // build query param
    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
      Object value = objects[i];

      if (requestParam != null && value != null) {
        String name = StringUtils.isEmpty(requestParam.value()) ? requestParam.name() : requestParam.value();
        builder.queryParam(name, value);
      }
    }

    // build path variable
    Map<String, Object> uriVariables = new HashMap<>();
    for (int i = 0; i < parameters.length; i++) {

      Parameter parameter = parameters[i];
      PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
      Object value = objects[i];

      if (pathVariable != null && value != null) {
        String name = StringUtils.isEmpty(pathVariable.value()) ? pathVariable.name() : pathVariable.value();
        uriVariables.put(name, value);
      }
    }

    return builder.build(uriVariables);
  }

  private String getPath(RequestMapping mapping) {
    if (mapping.value().length > 0) {
      return mapping.value()[0];
    } else {
      return mapping.path()[0];
    }
  }

  private WebClient.RequestHeadersUriSpec<?> doMethod(RequestMapping mapping) {
    RequestMethod[] methods = mapping.method();
    if (methods.length > 0) {
      RequestMethod method = methods[0];
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
      }
    }
    return webClient.get();
  }
}
