package eu.bbmri_eric.quality.agent.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.LenientErrorHandler;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import({BlazeTestConfig.class})
public class QualityCheckSystemTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private final IGenericClient client;
  private final String blazeFhirUrl =
      "http://" + blazeContainer.getHost() + ":" + blazeContainer.getMappedPort(8080) + "/fhir";
  static GenericContainer<?> blazeContainer =
      new GenericContainer<>("samply/blaze:latest")
          .withExposedPorts(8080)
          .waitingFor(Wait.forHttp("/fhir/metadata").forStatusCode(200));

  public QualityCheckSystemTest() {
    FhirContext ctx =
        FhirContext.forR4()
            .setParserErrorHandler(new LenientErrorHandler().setErrorOnInvalidValue(false));
    ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    client = ctx.newRestfulGenericClient(blazeFhirUrl);
    client.registerInterceptor(new PagingUrlInterceptor(blazeContainer.getMappedPort(8080)));
  }

  @DynamicPropertySource
  static void dynamicProperties(DynamicPropertyRegistry registry) {
    String fhirUrl =
        "http://" + blazeContainer.getHost() + ":" + blazeContainer.getMappedPort(8080) + "/fhir";
    registry.add("eu.bbmri_eric.quality.agent.fhir_url", () -> fhirUrl);
  }

  @BeforeAll
  static void startAndSeed() throws Exception {
    blazeContainer.start();
    RestTemplate restTemplate = new RestTemplate();

    List<String> resourceFiles =
        List.of(
            "biobank.json",
            "transaction-0.json",
            "transaction-100.json",
            "transaction-200.json",
            "transaction-300.json",
            "transaction-400.json",
            "transaction-500.json",
            "transaction-600.json",
            "transaction-700.json",
            "transaction-800.json",
            "transaction-900.json");

    Path baseDir = Paths.get(System.getProperty("user.dir"));
    Path testDataDir = baseDir.resolve("../../test_data").normalize();

    for (String filename : resourceFiles) {
      Path filePath = testDataDir.resolve(filename);

      if (!Files.exists(filePath)) {
        System.err.println("File not found: " + filePath);
        continue;
      }

      String json = Files.readString(filePath);
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<String> request = new HttpEntity<>(json, headers);
      String fhirUrl =
          "http://" + blazeContainer.getHost() + ":" + blazeContainer.getMappedPort(8080) + "/fhir";
      try {
        ResponseEntity<String> response =
            restTemplate.exchange(fhirUrl, HttpMethod.POST, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
          throw new RuntimeException(
              "Failed to upload " + filename + ": " + response.getStatusCode());
        }
      } catch (HttpClientErrorException e) {
        throw new RuntimeException("Http error: " + e.getStatusCode(), e);
      }
    }
  }

  @WithUserDetails("admin")
  @Test
  void systemTest_createsAndGeneratesReport() throws Exception {
    MvcResult createResult =
        mockMvc
            .perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isCreated())
            .andReturn();

    String reportUrl = createResult.getResponse().getHeader("Location");
    String reportPath = reportUrl.substring(reportUrl.indexOf("/api"));

    Instant start = Instant.now();
    String status;
    Integer numberOfEntities;
    do {
      if (Duration.between(start, Instant.now()).getSeconds() > 30) {
        throw new RuntimeException("Timeout: report was not generated in time");
      }
      Thread.sleep(1000);

      MvcResult getResult = mockMvc.perform(get(reportPath)).andExpect(status().isOk()).andReturn();

      String reportContent = getResult.getResponse().getContentAsString();
      Map<String, Object> report = objectMapper.readValue(reportContent, Map.class);
      status = (String) report.get("status");
      numberOfEntities = (Integer) report.get("numberOfEntities");
    } while (!"GENERATED".equals(status));

    assertThat(status).isEqualTo("GENERATED");
    assertThat(numberOfEntities).isNotNull();
    assertThat(numberOfEntities).isEqualTo(1000);
  }
}
