package eu.bbmri_eric.quality.agent.settings;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.settings.dto.DiffPrivacySettingsDTO;
import eu.bbmri_eric.quality.agent.settings.dto.FhirSettingsDTO;
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

  @Test
  @WithMockUser(username = "admin")
  void getSettings_shouldReturn200() throws Exception {
    mockMvc
        .perform(get("/api/settings"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.fhirUrl").exists())
        .andExpect(jsonPath("$.fhirUsername").exists())
        .andExpect(jsonPath("$.fhirPassword").exists());
  }

  @Test
  void getSettings_withoutAuthentication_shouldReturn401() throws Exception {
    mockMvc.perform(get("/api/settings")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "admin")
  void updateSettings_withInvalidUrl_shouldReturn400() throws Exception {
    FhirSettingsDTO settingsDTO = new FhirSettingsDTO("", "testuser", "dGVzdHBhc3M=");

    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(settingsDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "admin")
  void updateSettings_withNonHttpUrl_shouldReturn400() throws Exception {
    FhirSettingsDTO settingsDTO =
        new FhirSettingsDTO("ftp://localhost:8080/fhir", "testuser", "dGVzdHBhc3M=");

    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(settingsDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "admin")
  void updateSettings_withNonexistingSetting_shouldReturn500() throws Exception {
    Map<String, String> settings = new HashMap<>();
    settings.put("setting", "something");

    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(settings)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateSettings_withoutAuthentication_shouldReturn401() throws Exception {
    FhirSettingsDTO settingsDTO =
        new FhirSettingsDTO("http://localhost:8080/fhir", "testuser", "dGVzdHBhc3M=");

    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(settingsDTO)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "admin")
  void getDiffPrivacySettings_shouldReturn200() throws Exception {
    mockMvc
        .perform(get("/api/settings/differential-privacy"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.epsilon").exists())
        .andExpect(jsonPath("$.delta").exists())
        .andExpect(jsonPath("$.minThreshold").exists())
        .andExpect(jsonPath("$.noiseMechanism").exists());
  }

  @Test
  void getDiffPrivacySettings_withoutAuthentication_shouldReturn401() throws Exception {
    mockMvc.perform(get("/api/settings/differential-privacy")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "admin")
  void updateDiffPrivacySettings_withValidData_shouldReturn200() throws Exception {
    DiffPrivacySettingsDTO dto =
        new DiffPrivacySettingsDTO(2.0, 1.0E-8, 20, NoiseMechanism.GAUSSIAN);

    mockMvc
        .perform(
            put("/api/settings/differential-privacy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.epsilon").value(2.0))
        .andExpect(jsonPath("$.delta").value(1.0E-8))
        .andExpect(jsonPath("$.minThreshold").value(20))
        .andExpect(jsonPath("$.noiseMechanism").value("GAUSSIAN"));
  }
}
