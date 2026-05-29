package eu.bbmri_eric.quality.server.setting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SettingsControllerTest {

  public static final String API_V1_SETTINGS = "/api/v1/settings";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  @WithUserDetails("admin")
  void getSettings_withAuthentication_shouldReturn200() throws Exception {
    mockMvc
        .perform(get(API_V1_SETTINGS))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  void getSettings_withoutAuthentication_shouldReturn401() throws Exception {
    mockMvc.perform(get(API_V1_SETTINGS)).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getSettings_withAdminRole_shouldBeAccessible() throws Exception {
    mockMvc
        .perform(get(API_V1_SETTINGS))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  @WithMockUser(roles = "USER")
  void getSettings_withUserRole_shouldReturn403() throws Exception {
    mockMvc.perform(get(API_V1_SETTINGS)).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "USER")
  void patchSettings_withUserRole_shouldReturn403() throws Exception {
    mockMvc
        .perform(patch(API_V1_SETTINGS).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getSettings_withAdminRole_containsReportRetentionWithDefaultValue() throws Exception {
    mockMvc
        .perform(get(API_V1_SETTINGS))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.reportRetention").value(3));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void patchReportRetention_withAdminRole_updatesValue() throws Exception {
    SettingDTO dto = new SettingDTO();
    dto.setReportRetention(5);

    mockMvc
        .perform(
            patch(API_V1_SETTINGS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reportRetention").value(5));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void patchReportRetention_withInvalidValue_returnsBadRequest() throws Exception {
    SettingDTO dto = new SettingDTO();
    dto.setReportRetention(0);

    mockMvc
        .perform(
            patch(API_V1_SETTINGS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());
  }
}
