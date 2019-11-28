package com.blibli.oss.webclient.bean;

import com.blibli.oss.webclient.interceptor.ApiClientInterceptor;
import com.blibli.oss.webclient.properties.ApiClientProperties;
import lombok.Setter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

	@Override
	public void afterPropertiesSet() throws Exception {
		WebClient.Builder builder = WebClient.builder().baseUrl(url);
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
		RequestMapping mapping = method.getAnnotation(RequestMapping.class);
		RequestMethod requestMethod = mapping.method()[0];
		String[] consumes = mapping.consumes();
		String[] produces = mapping.produces();
		String[] headers = mapping.headers();

		String path = null;
		if (mapping.value().length > 0) {
			path = mapping.value()[0];
		} else {
			path = mapping.path()[0];
		}

		return Mono.<Object>fromCallable(() -> "Hello World")
			.onErrorResume(throwable -> Mono.create(sink -> {
				try {
					sink.success(invocation.proceed());
				} catch (Throwable e) {
					sink.error(e);
				}
			}));
	}
}
