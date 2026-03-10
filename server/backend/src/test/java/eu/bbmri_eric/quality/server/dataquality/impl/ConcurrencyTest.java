package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.quality.server.dataquality.AgentService;
import eu.bbmri_eric.quality.server.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.server.dataquality.domain.Category;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistration;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistrationRequest;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.server.util.CleanDatabaseAfter;
import eu.bbmri_eric.quality.server.util.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.test.context.support.WithMockUser;

@IntegrationTest
class ConcurrencyTest {

  private static final int THREAD_COUNT = 10;
  private static final int OPERATIONS_PER_THREAD = 5;
  private static final int TIMEOUT_SECONDS = 30;

  @Autowired private AgentService agentService;

  @Autowired private QualityCheckService qualityCheckService;

  @Autowired private AgentRepository agentRepository;

  @Autowired private QualityCheckRepository qualityCheckRepository;

  @Autowired private CategoryRepository categoryRepository;

  private List<String> qualityCheckHashes;

  @BeforeEach
  void setUp() {
    Category testCategory = new Category("Test Category", "#FF0000");
    categoryRepository.save(testCategory);

    qualityCheckHashes = new ArrayList<>();
    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
      String hash = "test-hash-" + i;
      QualityCheck qualityCheck =
          new QualityCheck(
              hash, "Quality Check " + i, "Test quality check for concurrency testing", 0.8, 0.5);
      qualityCheckRepository.save(qualityCheck);
      qualityCheckHashes.add(hash);
    }
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @CleanDatabaseAfter
  void concurrentAgentRegistration_shouldCreateAllAgentsSuccessfully() throws InterruptedException {
    ExecutorService executorService = createSecureExecutorService(THREAD_COUNT);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);

    Set<String> registeredAgentIds = ConcurrentHashMap.newKeySet();
    AtomicInteger successCount = new AtomicInteger(0);

    try {
      for (int i = 0; i < THREAD_COUNT; i++) {
        executorService.submit(
            () -> {
              try {
                startLatch.await();

                String agentId = UUID.randomUUID().toString();
                AgentRegistrationRequest request = new AgentRegistrationRequest(agentId, "1.0.0");
                AgentRegistration registration = agentService.create(request);

                registeredAgentIds.add(registration.getAgent().getId());
                successCount.incrementAndGet();

              } catch (Exception e) {
                // Registration failed
              } finally {
                endLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(
          endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
          "Threads should complete within timeout");

      assertEquals(
          THREAD_COUNT, successCount.get(), "All agent registration operations should succeed");
      assertEquals(
          THREAD_COUNT, registeredAgentIds.size(), "All registered agent IDs should be unique");
      assertEquals(
          THREAD_COUNT, agentRepository.count(), "Database should contain all registered agents");
    } finally {
      executorService.shutdown();
    }
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @CleanDatabaseAfter
  void concurrentQualityCheckReads_shouldReturnConsistentData() throws InterruptedException {
    ExecutorService executorService = createSecureExecutorService(THREAD_COUNT);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT * OPERATIONS_PER_THREAD);

    AtomicInteger successfulReads = new AtomicInteger(0);

    try {
      for (int i = 0; i < THREAD_COUNT; i++) {
        for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
          final int operationIndex = j;
          executorService.submit(
              () -> {
                try {
                  startLatch.await();

                  String hash = qualityCheckHashes.get(operationIndex);
                  QualityCheckDTO qualityCheckDTO = qualityCheckService.findById(hash);

                  assertEquals(
                      hash,
                      qualityCheckDTO.getHash(),
                      "Quality check hash should match requested hash");
                  assertEquals(
                      "Quality Check " + operationIndex,
                      qualityCheckDTO.getName(),
                      "Quality check name should be consistent");
                  assertEquals(
                      0.8,
                      qualityCheckDTO.getWarningThreshold(),
                      "Warning threshold should be consistent");
                  assertEquals(
                      0.5,
                      qualityCheckDTO.getErrorThreshold(),
                      "Error threshold should be consistent");

                  successfulReads.incrementAndGet();

                } catch (Exception e) {
                  // Read failed
                } finally {
                  endLatch.countDown();
                }
              });
        }
      }

      startLatch.countDown();
      assertTrue(
          endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
          "All read operations should complete within timeout");

      assertEquals(
          THREAD_COUNT * OPERATIONS_PER_THREAD,
          successfulReads.get(),
          "All quality check read operations should succeed");
    } finally {
      executorService.shutdown();
    }
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @CleanDatabaseAfter
  void concurrentFindAllQualityChecks_shouldReturnCompleteListConsistently()
      throws InterruptedException {
    ExecutorService executorService = createSecureExecutorService(THREAD_COUNT);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);

    AtomicInteger successfulReads = new AtomicInteger(0);
    List<Integer> resultSizes = new ArrayList<>();

    try {
      for (int i = 0; i < THREAD_COUNT; i++) {
        executorService.submit(
            () -> {
              try {
                startLatch.await();

                List<QualityCheckDTO> qualityChecks = qualityCheckService.findAll();

                synchronized (resultSizes) {
                  resultSizes.add(qualityChecks.size());
                }

                successfulReads.incrementAndGet();

              } catch (Exception e) {
                // Failure occurred
              } finally {
                endLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(
          endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
          "All operations should complete within timeout");

      assertEquals(THREAD_COUNT, successfulReads.get(), "All findAll operations should succeed");

      assertTrue(
          resultSizes.stream().allMatch(size -> size == OPERATIONS_PER_THREAD),
          "All findAll operations should return the same number of quality checks");
    } finally {
      executorService.shutdown();
    }
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @CleanDatabaseAfter
  void mixedConcurrentOperations_agentRegistrationAndQualityCheckReads_shouldNotInterfere()
      throws InterruptedException {
    ExecutorService executorService = createSecureExecutorService(THREAD_COUNT * 2);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT * 2);

    AtomicInteger agentRegistrations = new AtomicInteger(0);
    AtomicInteger qualityCheckReads = new AtomicInteger(0);

    try {
      for (int i = 0; i < THREAD_COUNT; i++) {
        final int threadIndex = i;

        executorService.submit(
            () -> {
              try {
                startLatch.await();

                String agentId = UUID.randomUUID().toString();
                AgentRegistrationRequest request = new AgentRegistrationRequest(agentId, "1.0.0");
                agentService.create(request);

                agentRegistrations.incrementAndGet();

              } catch (Exception e) {
                // Registration failed
              } finally {
                endLatch.countDown();
              }
            });

        executorService.submit(
            () -> {
              try {
                startLatch.await();

                String hash = qualityCheckHashes.get(threadIndex % OPERATIONS_PER_THREAD);
                qualityCheckService.findById(hash);

                qualityCheckReads.incrementAndGet();

              } catch (Exception e) {
                // Read failed
              } finally {
                endLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(
          endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
          "All operations should complete within timeout");

      assertEquals(
          THREAD_COUNT, agentRegistrations.get(), "All agent registrations should succeed");
      assertEquals(THREAD_COUNT, qualityCheckReads.get(), "All quality check reads should succeed");
    } finally {
      executorService.shutdown();
    }
  }

  private ExecutorService createSecureExecutorService(int threadCount) {
    ExecutorService baseExecutor = Executors.newFixedThreadPool(threadCount);
    return new DelegatingSecurityContextExecutorService(baseExecutor);
  }
}
