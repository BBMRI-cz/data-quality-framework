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
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestVersionCreateDTO;
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
  private static final String API_V1_MANIFEST_VERSIONS = "/api/v1/manifests/{id}/versions";

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
    testManifest = new Manifest("Quality Checks 2026-08");
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
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/manifests/" + testManifest.getId()))
        .andExpect(jsonPath("$._links.manifests.href").value("http://localhost/api/v1/manifests"));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnEmptyVersionsWhenNonePublished() throws Exception {
    mockMvc
        .perform(get(API_V1_MANIFESTS_ID, testManifest.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versions").isArray())
        .andExpect(jsonPath("$.versions.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findById_shouldReturnNotFoundWhenManifestDoesNotExist() throws Exception {
    mockMvc.perform(get(API_V1_MANIFESTS_ID, 99999L)).andExpect(status().isNotFound());
  }

  @Test
  void findById_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    mockMvc
        .perform(get(API_V1_MANIFESTS_ID, testManifest.getId()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void findAll_shouldReturnAllManifestsWithHateoasLinks() throws Exception {
    mockMvc
        .perform(get(API_V1_MANIFESTS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.manifests").isArray())
        .andExpect(jsonPath("$._embedded.manifests.length()").value(1))
        .andExpect(jsonPath("$._embedded.manifests[0].name").value("Quality Checks 2026-08"))
        .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/v1/manifests"));
  }

  @Test
  void findAll_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    mockMvc.perform(get(API_V1_MANIFESTS)).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldCreateManifestMetadataAndReturnCreatedStatus() throws Exception {
    ManifestCreateDTO createDTO = new ManifestCreateDTO("New Manifest");

    mockMvc
        .perform(
            post(API_V1_MANIFESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("New Manifest"))
        .andExpect(jsonPath("$.versions").isArray())
        .andExpect(jsonPath("$.versions.length()").value(0))
        .andExpect(jsonPath("$._links.manifests.href").value("http://localhost/api/v1/manifests"))
        .andExpect(jsonPath("$._links.self.href").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_shouldReturnBadRequestForBlankName() throws Exception {
    ManifestCreateDTO createDTO = new ManifestCreateDTO(" ");

    mockMvc
        .perform(
            post(API_V1_MANIFESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void create_shouldReturnForbiddenForNonAdminUser() throws Exception {
    ManifestCreateDTO createDTO = new ManifestCreateDTO("New Manifest");

    mockMvc
        .perform(
            post(API_V1_MANIFESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createVersion_shouldPublishSignedVersionWithAutoIncrementedVersion() throws Exception {
    ManifestVersionCreateDTO createDTO =
        new ManifestVersionCreateDTO(List.of(firstHash, secondHash), null);

    mockMvc
        .perform(
            post(API_V1_MANIFEST_VERSIONS, testManifest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.generatedAt").exists())
        .andExpect(jsonPath("$.body.manifest_id").value(testManifest.getId()))
        .andExpect(jsonPath("$.body.quality_checks").isArray())
        .andExpect(jsonPath("$.body.quality_checks.length()").value(2))
        .andExpect(
            jsonPath("$.body.quality_checks[0].check_id")
                .value(testQualityCheck.getId().toString()))
        .andExpect(jsonPath("$.body.quality_checks[0].version").value(7))
        .andExpect(jsonPath("$.body.quality_checks[0].sha256").value(firstHash))
        .andExpect(
            jsonPath("$.body.quality_checks[1].check_id")
                .value(secondQualityCheck.getId().toString()))
        .andExpect(jsonPath("$.body.quality_checks[1].version").value(3))
        .andExpect(
            jsonPath("$.signature")
                .value(java.util.Base64.getEncoder().encodeToString(new byte[] {1, 2, 3})))
        .andExpect(jsonPath("$.keyId").value("central-server-key"))
        .andExpect(
            jsonPath("$._links.manifest.href")
                .value("http://localhost/api/v1/manifests/" + testManifest.getId()));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createVersion_shouldIncrementVersionOnSubsequentPublishes() throws Exception {
    ManifestVersionCreateDTO createDTO = new ManifestVersionCreateDTO(List.of(firstHash), null);

    mockMvc
        .perform(
            post(API_V1_MANIFEST_VERSIONS, testManifest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.version").value(1));

    mockMvc
        .perform(
            post(API_V1_MANIFEST_VERSIONS, testManifest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.version").value(2));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createVersion_shouldAcceptExplicitVersionNumber() throws Exception {
    ManifestVersionCreateDTO createDTO = new ManifestVersionCreateDTO(List.of(firstHash), 7);

    mockMvc
        .perform(
            post(API_V1_MANIFEST_VERSIONS, testManifest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.version").value(7));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createVersion_shouldReturnConflictWhenVersionAlreadyExists() throws Exception {
    ManifestVersionCreateDTO createDTO = new ManifestVersionCreateDTO(List.of(firstHash), 1);

    mockMvc
        .perform(
            post(API_V1_MANIFEST_VERSIONS, testManifest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new ManifestVersionCreateDTO(List.of(firstHash), 1))))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post(API_V1_MANIFEST_VERSIONS, testManifest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createVersion_shouldReturnNotFoundWhenManifestDoesNotExist() throws Exception {
    ManifestVersionCreateDTO createDTO = new ManifestVersionCreateDTO(List.of(firstHash), null);

    mockMvc
        .perform(
            post(API_V1_MANIFEST_VERSIONS, 99999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createVersion_shouldReturnNotFoundWhenHashDoesNotMatchAnyVersion() throws Exception {
    ManifestVersionCreateDTO createDTO =
        new ManifestVersionCreateDTO(List.of("unknown-hash"), null);

    mockMvc
        .perform(
            post(API_V1_MANIFEST_VERSIONS, testManifest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createVersion_shouldReturnBadRequestForEmptyHashes() throws Exception {
    ManifestVersionCreateDTO createDTO = new ManifestVersionCreateDTO(List.of(), null);

    mockMvc
        .perform(
            post(API_V1_MANIFEST_VERSIONS, testManifest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createVersion_shouldFailWhenSigningFails() throws Exception {
    given(signatureService.sign(any(byte[].class)))
        .willThrow(new java.security.GeneralSecurityException("signing failed"));
    ManifestVersionCreateDTO createDTO = new ManifestVersionCreateDTO(List.of(firstHash), null);

    mockMvc
        .perform(
            post(API_V1_MANIFEST_VERSIONS, testManifest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.title").value("Signature Error"));
  }

  @Test
  @WithMockUser(roles = "HUMAN_USER")
  void createVersion_shouldReturnForbiddenForNonAdminUser() throws Exception {
    ManifestVersionCreateDTO createDTO = new ManifestVersionCreateDTO(List.of(firstHash), null);

    mockMvc
        .perform(
            post(API_V1_MANIFEST_VERSIONS, testManifest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isForbidden());
  }

  @Test
  void createVersion_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
    ManifestVersionCreateDTO createDTO = new ManifestVersionCreateDTO(List.of(firstHash), null);

    mockMvc
        .perform(
            post(API_V1_MANIFEST_VERSIONS, testManifest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findVersions_shouldReturnAllVersionsOrderedByVersionNumber() throws Exception {
    mockMvc
        .perform(
            post(API_V1_MANIFEST_VERSIONS, testManifest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new ManifestVersionCreateDTO(List.of(firstHash), null))))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post(API_V1_MANIFEST_VERSIONS, testManifest.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new ManifestVersionCreateDTO(List.of(secondHash), null))))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get(API_V1_MANIFEST_VERSIONS, testManifest.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.manifestVersions").isArray())
        .andExpect(jsonPath("$._embedded.manifestVersions.length()").value(2))
        .andExpect(jsonPath("$._embedded.manifestVersions[0].version").value(1))
        .andExpect(jsonPath("$._embedded.manifestVersions[1].version").value(2))
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/api/v1/manifests/" + testManifest.getId() + "/versions"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void findVersions_shouldReturnNotFoundWhenManifestDoesNotExist() throws Exception {
    mockMvc.perform(get(API_V1_MANIFEST_VERSIONS, 99999L)).andExpect(status().isNotFound());
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
