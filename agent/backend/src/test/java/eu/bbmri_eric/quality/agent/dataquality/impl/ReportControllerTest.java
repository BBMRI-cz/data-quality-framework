package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.quality.agent.dataquality.ReportPipeline;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportControllerTest {

  private static final String API_REPORTS = "/api/reports";

  @Autowired private MockMvc mockMvc;
  @Autowired private ReportRepository reportRepository;
  @MockitoBean private ReportPipeline reportPipeline;

  @BeforeEach
  void setUp() {
    reportRepository.deleteAll();
  }

  @Test
  @WithMockUser(username = "admin")
  void create_whenNoReportIsGenerating_returnsCreated() throws Exception {
    mockMvc
        .perform(post(API_REPORTS).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isCreated());
  }

  @Test
  @WithMockUser(username = "admin")
  void create_whenReportIsAlreadyGenerating_returnsConflict() throws Exception {
    Report generating = new Report();
    generating.setStatus(ReportStatus.GENERATING);
    reportRepository.save(generating);

    mockMvc
        .perform(post(API_REPORTS).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isConflict());
  }

  @Test
  void create_withoutAuthentication_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(post(API_REPORTS).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }
}
