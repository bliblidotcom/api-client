# Blibli API Client

Declarative API Client using Spring Web Client and Reactor Netty

## How to use It

```java

@ApiClient(
  name = "exampleClient"
)
public interface ExampleClient {

  @RequestMapping("/")
  Mono<String> get();

}

```

```properties
# application.properties
api.client.configs.exampleClient.url=http://example.com
api.client.configs.exampleClient.read-timeout=5s
api.client.configs.exampleClient.write-timeout=5s
api.client.configs.exampleClient.connect-timeout=5s
```

## Supported Annotation

- @RequestMapping
- @RequestBody
- @RequestParam
- @RequestHeader
- @PathVariable
- @CookieValue