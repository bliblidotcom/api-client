package com.blibli.oss.webclient.bean;

import com.blibli.oss.webclient.annotation.ApiClient;
import com.blibli.oss.webclient.interceptor.ApiClientInterceptor;
import com.blibli.oss.webclient.properties.ApiClientProperties;
import com.blibli.oss.webclient.properties.PropertiesHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class ApiClientMethodInterceptor implements MethodInterceptor, InitializingBean, ApplicationContextAware {

  @Setter
  private ApplicationContext applicationContext;

  @Setter
  private Class<?> type;

  @Setter
  private String name;

  private WebClient webClient;

  private Map<String, Method> methods;

  private Object fallback;

  private Map<String, MultiValueMap<String, String>> headers = new HashMap<>();

  private Map<String, Map<String, Integer>> queryParamPositions = new HashMap<>();

  private Map<String, Map<String, Integer>> headerParamPositions = new HashMap<>();

  private Map<String, Map<String, Integer>> pathVariablePositions = new HashMap<>();

  private Map<String, Integer> requestBodyPositions = new HashMap<>();

  private Map<String, RequestMethod> requestMethods = new HashMap<>();

  private Map<String, String> paths = new HashMap<>();

  private Map<String, Class> responseBodyClasses = new HashMap<>();

  private ApiClientProperties.ApiClientConfigProperties properties;

  @Override
  public void afterPropertiesSet() throws Exception {
    prepareProperties();
    prepareWebClient();
    prepareMethods();
    prepareHeaders();
    prepareQueryParams();
    prepareHeaderParams();
    preparePathVariables();
    prepareRequestBodies();
    prepareRequestBodyClasses();
    prepareRequestMethods();
    preparePaths();
    prepareFallback();
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
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.getConnectTimeout().toMillis())
      .doOnConnected(connection -> connection
        .addHandlerLast(new ReadTimeoutHandler(properties.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS))
        .addHandlerLast(new WriteTimeoutHandler(properties.getWriteTimeout().toMillis(), TimeUnit.MILLISECONDS))
      );

    webClient = WebClient.builder()
      .exchangeStrategies(strategies)
      .baseUrl(properties.getUrl())
      .clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient)))
      .defaultHeaders(httpHeaders -> properties.getHeaders().forEach(httpHeaders::add))
      .filters(exchangeFilterFunctions ->
        properties.getInterceptors().forEach(interceptorClass ->
          exchangeFilterFunctions.add((ApiClientInterceptor) applicationContext.getBean(interceptorClass))
        )
      )
      .build();
  }

  private void prepareProperties() {
    ApiClientProperties apiClientproperties = applicationContext.getBean(ApiClientProperties.class);
    properties = mergeApiClientConfigProperties(
      apiClientproperties.getConfigs().get(ApiClientProperties.DEFAULT),
      apiClientproperties.getConfigs().get(name)
    );
  }

  private ApiClientProperties.ApiClientConfigProperties mergeApiClientConfigProperties(ApiClientProperties.ApiClientConfigProperties defaultProperties,
                                                                                       ApiClientProperties.ApiClientConfigProperties properties) {
    ApiClientProperties.ApiClientConfigProperties configProperties = new ApiClientProperties.ApiClientConfigProperties();

    PropertiesHelper.copyConfigProperties(defaultProperties, configProperties);
    PropertiesHelper.copyConfigProperties(properties, configProperties);

    return configProperties;
  }

  private void prepareMethods() {
    methods = Arrays.stream(ReflectionUtils.getDeclaredMethods(type))
      .collect(Collectors.toMap(Method::getName, method -> method));
  }

  private void preparePaths() {
    methods.forEach((methodName, method) -> {
      RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
      if (requestMapping != null) {
        String[] pathValues = requestMapping.path().length > 0 ? requestMapping.path() : requestMapping.value();
        if (pathValues.length > 0) {
          paths.put(methodName, pathValues[0]);
        } else {
          paths.put(methodName, "");
        }
      }
    });
  }

  private void prepareRequestMethods() {
    methods.forEach((methodName, method) -> {
      RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
      if (requestMapping != null) {
        RequestMethod[] methods = requestMapping.method();
        if (methods.length > 0) {
          requestMethods.put(methodName, methods[0]);
        } else {
          requestMethods.put(methodName, RequestMethod.GET);
        }
      }
    });
  }

  private void prepareRequestBodies() {
    methods.forEach((methodName, method) -> {
      Parameter[] parameters = method.getParameters();
      for (int i = 0; i < parameters.length; i++) {
        Parameter parameter = parameters[i];
        RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
        if (requestBody != null) {
          requestBodyPositions.put(methodName, i);
        }
      }
    });
  }

  private void prepareRequestBodyClasses() {
    methods.forEach((methodName, method) -> {
      ParameterizedType parameterizedType = (ParameterizedType) method.getGenericReturnType();
      if (!parameterizedType.getRawType().getTypeName().equals(Mono.class.getName())) {
        throw new BeanCreationException(String.format("ApiClient method must return reactive, %s is not reactive", methodName));
      }

      Type[] typeArguments = parameterizedType.getActualTypeArguments();
      if (typeArguments.length != 1) {
        throw new BeanCreationException(String.format("ApiClient method must return 1 generic type, %s generic type is not 1", methodName));
      }

      try {
        responseBodyClasses.put(methodName, Class.forName(typeArguments[0].getTypeName()));
      } catch (ClassNotFoundException e) {
        throw new BeanCreationException(e.getMessage(), e);
      }
    });
  }

  private void prepareQueryParams() {
    methods.forEach((methodName, method) -> {
      RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
      if (requestMapping != null) {
        Parameter[] parameters = method.getParameters();
        Map<String, Integer> queryParamPosition = new HashMap<>();
        queryParamPositions.put(methodName, queryParamPosition);

        if (parameters.length > 0) {
          for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            RequestParam annotation = parameter.getAnnotation(RequestParam.class);
            if (annotation != null) {
              String name = StringUtils.isEmpty(annotation.name()) ? annotation.value() : annotation.name();
              if (!StringUtils.isEmpty(name)) {
                queryParamPosition.put(name, i);
              }
            }
          }
        }
      }
    });
  }

  private void prepareHeaderParams() {
    methods.forEach((methodName, method) -> {
      RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
      if (requestMapping != null) {
        Parameter[] parameters = method.getParameters();
        Map<String, Integer> headerParamPosition = new HashMap<>();
        headerParamPositions.put(methodName, headerParamPosition);

        if (parameters.length > 0) {
          for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            RequestHeader annotation = parameter.getAnnotation(RequestHeader.class);
            if (annotation != null) {
              String name = StringUtils.isEmpty(annotation.name()) ? annotation.value() : annotation.name();
              if (!StringUtils.isEmpty(name)) {
                headerParamPosition.put(name, i);
              }
            }
          }
        }
      }
    });
  }

  private void preparePathVariables() {
    methods.forEach((methodName, method) -> {
      RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
      if (requestMapping != null) {
        Parameter[] parameters = method.getParameters();
        Map<String, Integer> pathVariablePosition = new HashMap<>();
        pathVariablePositions.put(methodName, pathVariablePosition);

        if (parameters.length > 0) {
          for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            PathVariable annotation = parameter.getAnnotation(PathVariable.class);
            if (annotation != null) {
              String name = StringUtils.isEmpty(annotation.name()) ? annotation.value() : annotation.name();
              if (!StringUtils.isEmpty(name)) {
                pathVariablePosition.put(name, i);
              }
            }
          }
        }
      }
    });
  }

  private void prepareHeaders() {
    methods.forEach((methodName, method) -> {
      RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
      if (requestMapping != null) {
        HttpHeaders httpHeaders = new HttpHeaders();

        String[] consumes = requestMapping.consumes();
        if (consumes.length > 0) httpHeaders.addAll(HttpHeaders.CONTENT_TYPE, Arrays.asList(consumes));

        String[] produces = requestMapping.produces();
        if (produces.length > 0) httpHeaders.addAll(HttpHeaders.ACCEPT, Arrays.asList(produces));

        String[] requestHeaders = requestMapping.headers();
        for (String header : requestHeaders) {
          String[] split = header.split("=");
          if (split.length > 1) {
            httpHeaders.add(split[0], split[1]);
          } else {
            httpHeaders.add(split[0], "");
          }
        }
        headers.put(methodName, httpHeaders);
      }
    });
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    return Mono.fromCallable(() -> webClient)
      .map(client -> doMethod(invocation))
      .map(client -> client.uri(uriBuilder -> getUri(uriBuilder, invocation)))
      .map(client -> doHeader(client, invocation))
      .map(client -> doBody(client, invocation))
      .flatMap(client -> client.retrieve().bodyToMono(responseBodyClasses.get(invocation.getMethod().getName())))
      .onErrorResume(throwable -> doFallback((Throwable) throwable, invocation));
  }

  private WebClient.RequestHeadersUriSpec<?> doMethod(MethodInvocation invocation) {
    RequestMethod method = requestMethods.get(invocation.getMethod().getName());
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
    builder.path(paths.get(invocation.getMethod().getName()));

    queryParamPositions.get(invocation.getMethod().getName()).forEach((paramName, position) -> {
      builder.queryParam(paramName, invocation.getArguments()[position]);
    });

    Map<String, Object> uriVariables = new HashMap<>();
    pathVariablePositions.get(invocation.getMethod().getName()).forEach((paramName, position) -> {
      uriVariables.put(paramName, invocation.getArguments()[position]);
    });

    return builder.build(uriVariables);
  }

  private WebClient.RequestHeadersSpec<?> doHeader(WebClient.RequestHeadersSpec<?> spec, MethodInvocation invocation) {
    headers.get(invocation.getMethod().getName()).forEach((key, values) -> {
      spec.headers(httpHeaders -> httpHeaders.addAll(key, values));
    });

    headerParamPositions.get(invocation.getMethod().getName()).forEach((key, position) -> {
      spec.headers(httpHeaders -> httpHeaders.add(key, String.valueOf(invocation.getArguments()[position])));
    });

    return spec;
  }

  private WebClient.RequestHeadersSpec<?> doBody(WebClient.RequestHeadersSpec<?> client, MethodInvocation invocation) {
    if (client instanceof WebClient.RequestBodySpec) {
      Integer bodyPosition = requestBodyPositions.get(invocation.getMethod().getName());
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
