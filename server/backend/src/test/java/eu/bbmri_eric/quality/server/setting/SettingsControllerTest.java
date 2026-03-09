package eu.bbmri_eric.quality.server.setting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
  void getSettings_withUserRole_shouldBeAccessible() throws Exception {
    mockMvc
        .perform(get(API_V1_SETTINGS))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }
}
