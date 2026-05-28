package eu.bbmri_eric.quality.agent.settings;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
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
    return SettingsDTO.builder()
        .fhirUrl("http://localhost:8080/fhir")
        .fhirUsername("testuser")
        .fhirPassword("dGVzdHBhc3M=")
        .epsilon(0.5)
        .delta(1.0E-8)
        .minThreshold(20)
        .noiseMechanism(NoiseMechanism.GAUSSIAN)
        .databaseType(DatabaseType.FHIR)
        .build();
  }

  private SettingsDTO validSqlSettingsDTO() {
    return SettingsDTO.builder()
        .databaseType(DatabaseType.SQL)
        .sqlUrl("jdbc:postgresql://localhost:5432/quality")
        .sqlUsername("dbuser")
        .sqlPassword("c2VjcmV0")
        .epsilon(0.5)
        .delta(1.0E-8)
        .minThreshold(20)
        .noiseMechanism(NoiseMechanism.GAUSSIAN)
        .build();
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
        .andExpect(jsonPath("$.noiseMechanism").exists())
        .andExpect(jsonPath("$.databaseType").exists());
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
        .andExpect(jsonPath("$.epsilon").value(validSettingsDTO().getEpsilon()))
        .andExpect(jsonPath("$.noiseMechanism").value("GAUSSIAN"));
  }

  @Test
  @WithMockUser(username = "admin")
  void updateSettings_withGaussianAndEpsilonAboveOne_shouldReturn400() throws Exception {
    SettingsDTO dto = validSettingsDTO();
    dto.setEpsilon(2.0);
    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(get("/api/settings"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.noiseMechanism").value("GAUSSIAN"))
        .andExpect(jsonPath("$.epsilon").value(0.5));
    dto.setNoiseMechanism(NoiseMechanism.LAPLACE);
    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.noiseMechanism").value("LAPLACE"))
        .andExpect(jsonPath("$.epsilon").value(2.0));
    ;
    dto.setNoiseMechanism(NoiseMechanism.GAUSSIAN);
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
  void updateSettings_withEmptyFhirUrl_shouldReturn200() throws Exception {
    SettingsDTO dto = validSettingsDTO();
    dto.setFhirUrl("");
    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk());
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

  @Test
  @WithMockUser(username = "admin")
  void updateSettings_withSqlSettings_shouldReturn200() throws Exception {
    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSqlSettingsDTO())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.databaseType").value("SQL"))
        .andExpect(jsonPath("$.sqlUrl").value("jdbc:postgresql://localhost:5432/quality"));
  }

  @Test
  @WithMockUser(username = "admin")
  void updateSettings_withSqlInvalidUrl_shouldReturn400() throws Exception {
    SettingsDTO dto = validSqlSettingsDTO();
    dto.setSqlUrl("http://localhost:5432/quality");

    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "admin")
  void updateSettings_withSqlEmptyUrl_shouldReturn400() throws Exception {
    SettingsDTO dto = validSqlSettingsDTO();
    dto.setSqlUrl("sdfsdf sfs ");

    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "admin")
  void updateSettings_withSqlUsernameTooLong_shouldReturn400() throws Exception {
    SettingsDTO dto = validSqlSettingsDTO();
    dto.setSqlUsername("a".repeat(101));

    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "admin")
  void updateSettings_withSqlPasswordTooLong_shouldReturn400() throws Exception {
    SettingsDTO dto = validSqlSettingsDTO();
    dto.setSqlPassword("a".repeat(101));

    mockMvc
        .perform(
            put("/api/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }
}
