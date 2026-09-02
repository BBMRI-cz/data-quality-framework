package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.server.crypto.KeyProvider;
import eu.bbmri_eric.quality.server.crypto.SignatureService;
import eu.bbmri_eric.quality.server.dataquality.domain.Manifest;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckVersion;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestCreateDTO;
import eu.bbmri_eric.quality.server.util.IntegrationTest;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ManifestControllerTest {

  private static final String API_V1_MANIFESTS = "/api/v1/manifests";
  private static final String API_V1_MANIFESTS_ID = "/api/v1/manifests/{id}";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ManifestRepository manifestRepository;
  @Autowired private QualityCheckRepository qualityCheckRepository;
  @Autowired private EntityManager entityManager;

  @MockitoBean private SignatureService signatureService;
  @MockitoBean private KeyProvider keyProvider;

  private Manifest testManifest;
  private QualityCheck testQualityCheck;
  private QualityCheck secondQualityCheck;
  private String firstHash;
  private String secondHash;

  @BeforeEach
  void setUp() throws Exception {
    given(signatureService.sign(any(byte[].class))).willReturn(new byte[] {1, 2, 3});
    given(keyProvider.getKeyId()).willReturn("central-server-key");

    manifestRepository.deleteAll();
    testManifest = new Manifest("Quality Checks 2026-08", "{\"checks\":[]}", null, null);
    testManifest = manifestRepository.save(testManifest);

    testQualityCheck = new QualityCheck("Data Completeness", "Checks completeness of records");
    testQualityCheck.addVersion(
        new QualityCheckVersion(
            testQualityCheck, 7, "SELECT COUNT(*) FROM patients WHERE gender = 'F'"));
    firstHash = hashOf("SELECT COUNT(*) FROM patients WHERE gender = 'F'");
    qualityCheckRepository.save(testQualityCheck);

    secondQualityCheck = new QualityCheck("Data Accuracy", "Checks accuracy of records");
    secondQualityCheck.addVersion(
        new QualityCheckVersion(
            secondQualityCheck, 3, "SELECT COUNT(*) FROM patients WHERE gender = 'M'"));
    secondHash = hashOf("SELECT COUNT(*) FROM patients WHERE gender = 'M'");
    qualityCheckRepository.save(secondQualityCheck);

    entityManager.flush();
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnManifestWithHateoasLinksWhenExists() throws Exception {
    mockMvc
        .perform(get(API_V1_MANIFESTS_ID, testManifest.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testManifest.getId()))
        .andExpect(jsonPath("$.name").value("Quality Checks 2026-08"))
        .andExpect(jsonPath("$.generatedAt").exists())
        .andExpect(jsonPath("$.body").exists())
        .andExpect(jsonPath("$.body.checks").isArray())
        .andExpect(jsonPath("$.body.checks.length()").value(0))
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/manifests/" + testManifest.getId()))
        .andExpect(jsonPath("$._links.manifests.href").value("http://localhost/api/v1/manifests"));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnNotFoundWhenManifestDoesNotExist() throws Exception {
    Long nonExistentId = 99999L;

    mockMvc.perform(get(API_V1_MANIFESTS_ID, nonExistentId)).andExpect(status().isNotFound());
  }

  @Test
  void findById_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    mockMvc
        .perform(get(API_V1_MANIFESTS_ID, testManifest.getId()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findAll_shouldReturnEmptyCollectionWhenNoManifests() throws Exception {
    manifestRepository.deleteAll();

    mockMvc
        .perform(get(API_V1_MANIFESTS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded").doesNotExist())
        .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v1/manifests"));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findAll_shouldReturnAllManifestsWithHateoasLinks() throws Exception {
    Manifest secondManifest = new Manifest("Second Manifest", "{\"checks\":[1]}", null, null);
    manifestRepository.save(secondManifest);

    mockMvc
        .perform(get(API_V1_MANIFESTS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.manifests").isArray())
        .andExpect(jsonPath("$._embedded.manifests.length()").value(2))
        .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v1/manifests"))
        .andExpect(jsonPath("$._embedded.manifests[0]._links.self.href").exists())
        .andExpect(jsonPath("$._embedded.manifests[1]._links.self.href").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldCreateManifestAndReturnCreatedStatusWithHateoasLinks() throws Exception {
    ManifestCreateDTO createDTO =
        new ManifestCreateDTO("Quality Checks 2026-08", List.of(firstHash, secondHash));

    mockMvc
        .perform(
            post(API_V1_MANIFESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Quality Checks 2026-08"))
        .andExpect(jsonPath("$.generatedAt").exists())
        .andExpect(jsonPath("$.body.manifest_id").exists())
        .andExpect(jsonPath("$.body.checks").isArray())
        .andExpect(jsonPath("$.body.checks.length()").value(2))
        .andExpect(jsonPath("$.body.checks[0].check_id").value(testQualityCheck.getId().toString()))
        .andExpect(jsonPath("$.body.checks[0].version").value(7))
        .andExpect(jsonPath("$.body.checks[0].sha256").value(firstHash))
        .andExpect(
            jsonPath("$.body.checks[1].check_id").value(secondQualityCheck.getId().toString()))
        .andExpect(jsonPath("$.body.checks[1].version").value(3))
        .andExpect(jsonPath("$.body.checks[1].sha256").value(secondHash))
        .andExpect(
            jsonPath("$.signature")
                .value(java.util.Base64.getEncoder().encodeToString(new byte[] {1, 2, 3})))
        .andExpect(jsonPath("$.keyId").value("central-server-key"))
        .andExpect(jsonPath("$._links.manifests.href").value("http://localhost/api/v1/manifests"))
        .andExpect(jsonPath("$._links.self.href").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldReturnNotFoundWhenHashDoesNotMatchAnyVersion() throws Exception {
    ManifestCreateDTO createDTO =
        new ManifestCreateDTO("Quality Checks 2026-08", List.of("unknown-hash"));

    mockMvc
        .perform(
            post(API_V1_MANIFESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldReturnBadRequestForEmptyHashes() throws Exception {
    ManifestCreateDTO createDTO = new ManifestCreateDTO("Quality Checks 2026-08", List.of());

    mockMvc
        .perform(
            post(API_V1_MANIFESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldReturnBadRequestForBlankName() throws Exception {
    ManifestCreateDTO createDTO = new ManifestCreateDTO(" ", List.of(firstHash));

    mockMvc
        .perform(
            post(API_V1_MANIFESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldFailWhenSigningFails() throws Exception {
    given(signatureService.sign(any(byte[].class)))
        .willThrow(new java.security.GeneralSecurityException("signing failed"));
    ManifestCreateDTO createDTO =
        new ManifestCreateDTO("Quality Checks 2026-08", List.of(firstHash));

    mockMvc
        .perform(
            post(API_V1_MANIFESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.title").value("Signature Error"));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void create_shouldReturnForbiddenForNonAdminUser() throws Exception {
    ManifestCreateDTO createDTO =
        new ManifestCreateDTO("Quality Checks 2026-08", List.of(firstHash));

    mockMvc
        .perform(
            post(API_V1_MANIFESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  void create_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    ManifestCreateDTO createDTO =
        new ManifestCreateDTO("Quality Checks 2026-08", List.of(firstHash));

    mockMvc
        .perform(
            post(API_V1_MANIFESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isUnauthorized());
  }

  private static String hashOf(String query) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(query.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
