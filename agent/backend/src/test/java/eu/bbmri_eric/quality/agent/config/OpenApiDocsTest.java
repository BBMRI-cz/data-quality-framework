package eu.bbmri_eric.quality.agent.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** Integration tests verifying that the OpenAPI documentation is generated and served. */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

  private static final String API_DOCS_ENDPOINT = "/api/api-docs";

  @Autowired private MockMvc mockMvc;

  @Test
  void getApiDocs_withoutAuthentication_shouldReturn200() throws Exception {
    mockMvc.perform(get(API_DOCS_ENDPOINT)).andExpect(status().isOk());
  }

  @Test
  void getApiDocs_shouldContainApiTitle() throws Exception {
    mockMvc
        .perform(get(API_DOCS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.title").value("Data Quality Agent API"))
        .andExpect(jsonPath("$.info.version").value("1.0.0"));
  }

  @Test
  void getApiDocs_shouldDefineBearerAuthScheme() throws Exception {
    mockMvc
        .perform(get(API_DOCS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
  }

  @Test
  void getApiDocs_shouldExposeProtectedEndpoint() throws Exception {
    mockMvc
        .perform(get(API_DOCS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths./api/categories.get").exists());
  }

  @Test
  void getApiDocs_shouldExposePublicLoginEndpoint() throws Exception {
    mockMvc
        .perform(get(API_DOCS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths./api/auth/login.post").exists());
  }
}
