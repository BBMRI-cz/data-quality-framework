package eu.bbmri_eric.quality.agent.settings;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SettingsControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private SettingsDTO validSettingsDTO() {
    SettingsDTO dto = new SettingsDTO();
    dto.setFhirUrl("http://localhost:8080/fhir");
    dto.setFhirUsername("testuser");
    dto.setFhirPassword("dGVzdHBhc3M=");
    dto.setEpsilon(2.0);
    dto.setDelta(1.0E-8);
    dto.setMinThreshold(20);
    dto.setNoiseMechanism(NoiseMechanism.GAUSSIAN);
    return dto;
  }

  @Test
  @WithMockUser(username = "admin")
  void getSettings_shouldReturn200WithAllFields() throws Exception {
    mockMvc
        .perform(get("/api/settings"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.fhirUrl").exists())
        .andExpect(jsonPath("$.fhirUsername").exists())
        .andExpect(jsonPath("$.fhirPassword").exists())
        .andExpect(jsonPath("$.epsilon").exists())
        .andExpect(jsonPath("$.delta").exists())
        .andExpect(jsonPath("$.minThreshold").exists())
        .andExpect(jsonPath("$.noiseMechanism").exists());
  }

  @Test
  void getSettings_withoutAuthentication_shouldReturn401() throws Exception {
    mockMvc.perform(get("/api/settings")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "admin")
  void updateSettings_withValidData_shouldReturn200() throws Exception {
    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSettingsDTO())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fhirUrl").value("http://localhost:8080/fhir"))
        .andExpect(jsonPath("$.epsilon").value(2.0))
        .andExpect(jsonPath("$.noiseMechanism").value("GAUSSIAN"));
  }

  @Test
  @WithMockUser(username = "admin")
  void updateSettings_withInvalidUrl_shouldReturn400() throws Exception {
    SettingsDTO dto = validSettingsDTO();
    dto.setFhirUrl("");

    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "admin")
  void updateSettings_withNonHttpUrl_shouldReturn400() throws Exception {
    SettingsDTO dto = validSettingsDTO();
    dto.setFhirUrl("ftp://localhost:8080/fhir");

    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "admin")
  void updateSettings_withMissingRequiredFields_shouldReturn400() throws Exception {
    Map<String, String> incomplete = new HashMap<>();
    incomplete.put("fhirUrl", "http://localhost:8080/fhir");

    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(incomplete)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateSettings_withoutAuthentication_shouldReturn401() throws Exception {
    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSettingsDTO())))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "admin")
  void updateSettings_withNegativeEpsilon_shouldReturn400() throws Exception {
    SettingsDTO dto = validSettingsDTO();
    dto.setEpsilon(-1.0);

    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "admin")
  void updateSettings_withNegativeMinThreshold_shouldReturn400() throws Exception {
    SettingsDTO dto = validSettingsDTO();
    dto.setMinThreshold(-1);

    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }
}
