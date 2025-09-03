package eu.bbmri_eric.quality.agent.report;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.user.TestUserSeedConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestUserSeedConfig.class)
public class ReportIntegrationTests {
  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void testCreateReportWithStatusGeneratingSetsGeneratedAt() throws Exception {
    Report report = new Report();

    String json = objectMapper.writeValueAsString(report);

    String location =
        mockMvc
            .perform(
                post("/api/reports")
                    .with(httpBasic("admin", "pass"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertNotNull(location);

    mockMvc
        .perform(get(location).with(httpBasic("admin", "pass")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("GENERATED"))
        .andExpect(jsonPath("$.generatedAt").exists());
  }

  @Test
  void testGetReportsReturnsEmbeddedList() throws Exception {
    mockMvc
        .perform(get("/api/reports").with(httpBasic("admin", "pass")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.reports").exists());
  }
}
