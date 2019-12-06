# Blibli API Client

Declarative API Client using Spring Web Client and Reactor Netty

## Dependency

```xml

<dependencies>
    <dependency>
        <groupId>com.blibli.oss</groupId>
        <artifactId>api-client</artifactId>
        <version>${api-client.version}</version>
    </dependency>
</dependencies>

<repository>
  <snapshots>
    <enabled>false</enabled>
  </snapshots>
  <id>bintray-bliblidotcom-maven</id>
  <name>bintray</name>
  <url>https://dl.bintray.com/bliblidotcom/maven</url>
</repository>
```

## How to use It

```java
@EnableApiClient
@SpringBootApplication
public class YourApplication {

}
```

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
- @RequestPart