package it.gov.pagopa.gpd.rtp.config;

import static it.gov.pagopa.gpd.rtp.util.Constants.HEADER_REQUEST_ID;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI(
      @Value("${info.application.artifactId}") String appName,
      @Value("${info.application.description}") String appDescription,
      @Value("${info.application.version}") String appVersion) {
    return new OpenAPI()
        .servers(
            List.of(
                new Server().url("http://localhost:8080"),
                new Server()
                    .url("https://{host}{basePath}")
                    .variables(
                        new ServerVariables()
                            .addServerVariable(
                                "host",
                                new ServerVariable()
                                    ._enum(List.of("dev", "uat", "prod")) // TODO: set server hosts
                                    ._default("")) // TODO: set default server host
                            .addServerVariable(
                                "basePath",
                                new ServerVariable()._default("")) // TODO: set app base path
                        )))
        .components(
            new Components()
                .addSecuritySchemes(
                    "ApiKey",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .description("The API key to access this function app.")
                        .name("Ocp-Apim-Subscription-Key")
                        .in(SecurityScheme.In.HEADER)))
        .info(
            new Info()
                .title(appName)
                .version(appVersion)
                .description(appDescription)
                .termsOfService("https://www.pagopa.gov.it/"));
  }

  @Bean
  public GlobalOpenApiCustomizer sortOperationsAlphabetically() {
    return openApi -> {
      Paths paths =
          openApi.getPaths().entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .collect(
                  Paths::new,
                  (map, item) -> map.addPathItem(item.getKey(), item.getValue()),
                  Paths::putAll);

      paths.forEach(
          (key, value) ->
              value
                  .readOperations()
                  .forEach(
                      operation -> {
                        var responses =
                            operation.getResponses().entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .collect(
                                    ApiResponses::new,
                                    (map, item) ->
                                        map.addApiResponse(item.getKey(), item.getValue()),
                                    ApiResponses::putAll);
                        operation.setResponses(responses);
                      }));
      openApi.setPaths(paths);
    };
  }

  @Bean
  public GlobalOpenApiCustomizer addCommonHeaders() {
    return openApi ->
        openApi
            .getPaths()
            .forEach(
                (key, value) -> {

                  // add Request-ID as request header
                  var header =
                      Optional.ofNullable(value.getParameters())
                          .orElse(Collections.emptyList())
                          .parallelStream()
                          .filter(Objects::nonNull)
                          .anyMatch(elem -> HEADER_REQUEST_ID.equals(elem.getName()));
                  if (!header) {
                    value.addParametersItem(
                        new Parameter()
                            .in("header")
                            .name(HEADER_REQUEST_ID)
                            .schema(new StringSchema())
                            .description(
                                "This header identifies the call, if not passed it is"
                                    + " self-generated. This ID is returned in the response."));
                  }

                  // add Request-ID as response header
                  value
                      .readOperations()
                      .forEach(
                          operation ->
                              operation
                                  .getResponses()
                                  .values()
                                  .forEach(
                                      response ->
                                          response.addHeaderObject(
                                              HEADER_REQUEST_ID,
                                              new Header()
                                                  .schema(new StringSchema())
                                                  .description(
                                                      "This header identifies the call"))));
                });
  }
}
