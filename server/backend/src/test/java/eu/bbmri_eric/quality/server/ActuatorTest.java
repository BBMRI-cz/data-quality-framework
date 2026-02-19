package eu.bbmri_eric.quality.server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** Integration tests for actuator health and info endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorTest {

  private static final String HEALTH_ENDPOINT = "/api/health";
  private static final String INFO_ENDPOINT = "/api/info";

  @Autowired private MockMvc mockMvc;

  @Test
  void getHealth_withoutAuthentication_shouldReturn200() throws Exception {
    mockMvc
        .perform(get(HEALTH_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").exists());
  }

  @Test
  void getHealth_shouldReturnStatusUp() throws Exception {
    mockMvc
        .perform(get(HEALTH_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void getInfo_withoutAuthentication_shouldReturn200() throws Exception {
    mockMvc.perform(get(INFO_ENDPOINT)).andExpect(status().isOk());
  }

  @Test
  void getInfo_shouldContainGitInformation() throws Exception {
    mockMvc
        .perform(get(INFO_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.git").exists());
  }

  @Test
  void getInfo_shouldContainBuildInformation() throws Exception {
    mockMvc
        .perform(get(INFO_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.build").exists());
  }
}
