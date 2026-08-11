package eu.bbmri_eric.quality.agent.logs.impl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.user.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class LogControllerTest {

  private static final Logger logger = LoggerFactory.getLogger(LogControllerTest.class);
  private static final String AUTH_LOGIN_ENDPOINT = "/api/auth/login";
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASS = "adminpass";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void getRecentLogs_withoutToken_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/logs")).andExpect(status().isUnauthorized());
  }

  @Test
  void getRecentLogs_withValidToken_returnsLatestLogs() throws Exception {
    String token = authenticateAndGetToken();
    String marker = "LogControllerTest marker message for log capture";
    logger.info(marker);

    mockMvc
        .perform(get("/api/logs").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[?(@.message == '" + marker + "')]").isNotEmpty());
  }

  private String authenticateAndGetToken() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post(AUTH_LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(new LoginRequest(ADMIN_USER, ADMIN_PASS))))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }
}
