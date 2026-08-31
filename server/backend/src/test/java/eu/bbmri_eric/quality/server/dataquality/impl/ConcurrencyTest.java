package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistrationRequest;
import eu.bbmri_eric.quality.server.util.IntegrationTest;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class ConcurrencyTest {

  private static final Logger log = LoggerFactory.getLogger(ConcurrencyTest.class);
  private static final int THREAD_COUNT = 10;
  private static final int TIMEOUT_SECONDS = 30;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AgentRepository agentRepository;
  @Autowired private QualityCheckRepository qualityCheckRepository;

  @Test
  @WithMockUser(roles = "ADMIN")
  void concurrentAgentRegistration_shouldCreateAllAgentsSuccessfully() throws Exception {
    long initialAgentCount = agentRepository.count();
    log.info("Initial agent count: {}", initialAgentCount);

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
    Set<String> registeredAgentIds = ConcurrentHashMap.newKeySet();
    AtomicInteger successCount = new AtomicInteger(0);
    ExecutorService executorService =
        new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(THREAD_COUNT));

    try {
      for (int i = 0; i < THREAD_COUNT; i++) {
        int finalI = i;
        executorService.submit(
            () -> {
              try {
                startLatch.await();
                String agentId = UUID.randomUUID().toString();
                AgentRegistrationRequest request = new AgentRegistrationRequest(agentId, "1.0.0");

                mockMvc
                    .perform(
                        post("/api/v1/agents")
                            .with(
                                mockHttpServletRequest -> {
                                  mockHttpServletRequest.setRemoteAddr(
                                      "192.168.1.%d".formatted(finalI));
                                  return mockHttpServletRequest;
                                })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

                registeredAgentIds.add(agentId);
                successCount.incrementAndGet();
              } catch (Exception e) {
                log.error("Agent registration failed", e);
              } finally {
                endLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

      long finalAgentCount = agentRepository.count();
      log.info(
          "Final agent count: {}, New agents: {}",
          finalAgentCount,
          finalAgentCount - initialAgentCount);

      assertEquals(THREAD_COUNT, successCount.get());
      assertEquals(THREAD_COUNT, registeredAgentIds.size());
      assertEquals(initialAgentCount + THREAD_COUNT, finalAgentCount);
    } finally {
      executorService.shutdown();
    }
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void concurrentQualityCheckReads_shouldReturnConsistentData() throws Exception {
    long initialQualityCheckCount = qualityCheckRepository.count();
    log.info("Initial quality check count: {}", initialQualityCheckCount);

    qualityCheckRepository.save(new QualityCheck("QC 1", "Description 1", 0.8, 0.5));
    qualityCheckRepository.save(new QualityCheck("QC 2", "Description 2", 0.7, 0.4));
    qualityCheckRepository.save(new QualityCheck("QC 3", "Description 3", 0.9, 0.6));

    long afterSetupCount = qualityCheckRepository.count();
    log.info(
        "Quality checks after setup: {}, Added: {}",
        afterSetupCount,
        afterSetupCount - initialQualityCheckCount);

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
    AtomicInteger successfulReads = new AtomicInteger(0);
    ExecutorService executorService =
        new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(THREAD_COUNT));

    try {
      for (int i = 0; i < THREAD_COUNT; i++) {
        executorService.submit(
            () -> {
              try {
                startLatch.await();
                mockMvc.perform(get("/api/v1/quality-checks")).andExpect(status().isOk());
                successfulReads.incrementAndGet();
              } catch (Exception e) {
                log.error("Quality check read failed", e);
              } finally {
                endLatch.countDown();
              }
            });
      }
      startLatch.countDown();
      assertTrue(endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
      log.info("Successful reads: {} out of {}", successfulReads.get(), THREAD_COUNT);
      assertEquals(THREAD_COUNT, successfulReads.get());
    } finally {
      executorService.shutdown();
    }
  }
}
