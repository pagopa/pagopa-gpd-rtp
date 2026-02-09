package it.gov.pagopa.gpd.rtp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "redis.host=localhost",
        "redis.port=6379",
        "redis.password=fake",
        "info.application.version=1.0.0",
        "spring.data.redis.repositories.enabled=false",
        "spring.cloud.stream.enabled=false"
})
class OpenApiGenerationTest {

  @Autowired private MockMvc mvc;

  @MockBean private org.springframework.data.redis.connection.jedis.JedisConnectionFactory jedisConnectionFactory;
  @MockBean private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
  @MockBean private org.springframework.data.redis.listener.RedisMessageListenerContainer redisMessageListenerContainer;

  @Test
  void swaggerSpringPlugin() throws Exception {
    saveOpenAPI("/v3/api-docs", "openapi.json");
  }

  private void saveOpenAPI(String fromUri, String toFile) throws Exception {
    mvc.perform(MockMvcRequestBuilders.get(fromUri).accept(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
        .andDo(
            (result) -> {
              assertNotNull(result);
              assertNotNull(result.getResponse());
              final String content = result.getResponse().getContentAsString();
              assertFalse(content.isBlank());
              assertFalse(content.contains("${"), "Generated swagger contains placeholders");
              assertFalse(
                  content.contains("@some.value@)"), "Generated swagger contains placeholders");
              ObjectMapper objectMapper = new ObjectMapper();
              Object swagger =
                  objectMapper.readValue(result.getResponse().getContentAsString(), Object.class);
              String formatted =
                  objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(swagger);
              Path basePath = Paths.get("openapi/");
              Files.createDirectories(basePath);
              Files.write(basePath.resolve(toFile), formatted.getBytes());
            });
  }
}
