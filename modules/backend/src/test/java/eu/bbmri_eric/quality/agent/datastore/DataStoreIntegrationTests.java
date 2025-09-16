package eu.bbmri_eric.quality.agent.datastore;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class DataStoreIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @TestConfiguration
  static class TestConfig {
    @Bean
    public DataStore dataStore() {
      return new DataStore() {
        @Override
        public JSONObject getEntity(String entityType, String id) throws Exception {
          if ("fail".equals(id)) throw new RuntimeException("DB error");
          return new JSONObject().put("id", id).put("type", entityType);
        }

        @Override
        public JSONObject checkHealth() throws Exception {
          return new JSONObject().put("status", "healthy");
        }
      };
    }
  }

  @Test
  void getEntity_returnsEntityJson() throws Exception {
    mockMvc
        .perform(get("/api/entities/Patient/abc").with(httpBasic("admin", "pass")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("\"id\":\"abc\"")))
        .andExpect(content().string(containsString("\"type\":\"Patient\"")));
  }

  @Test
  void getEntity_returnsErrorOnException() throws Exception {
    mockMvc
        .perform(get("/api/entities/Patient/fail").with(httpBasic("admin", "pass")))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string(containsString("Failed to retrieve entity")));
  }
}
