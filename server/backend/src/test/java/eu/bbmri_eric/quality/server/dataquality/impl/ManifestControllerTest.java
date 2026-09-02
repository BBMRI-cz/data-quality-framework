package eu.bbmri_eric.quality.server.dataquality.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.server.crypto.KeyProvider;
import eu.bbmri_eric.quality.server.crypto.SignatureService;
import eu.bbmri_eric.quality.server.dataquality.domain.Manifest;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestCreateDTO;
import eu.bbmri_eric.quality.server.util.IntegrationTest;
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

  @MockitoBean private SignatureService signatureService;
  @MockitoBean private KeyProvider keyProvider;

  private Manifest testManifest;

  @BeforeEach
  void setUp() throws Exception {
    given(signatureService.sign(any(byte[].class))).willReturn(new byte[] {1, 2, 3});
    given(keyProvider.getKeyId()).willReturn("central-server-key");
    manifestRepository.deleteAll();
    testManifest = new Manifest("Quality Checks 2026-08", "{\"checks\":[]}", null, null);
    testManifest = manifestRepository.save(testManifest);
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
        .andExpect(jsonPath("$.body").value("{\"checks\":[]}"))
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
        new ManifestCreateDTO("Quality Checks 2026-08", List.of("5f3c9a...", "b4d2e..."));

    mockMvc
        .perform(
            post(API_V1_MANIFESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Quality Checks 2026-08"))
        .andExpect(jsonPath("$.generatedAt").exists())
        .andExpect(jsonPath("$.body").value("[\"5f3c9a...\",\"b4d2e...\"]"))
        .andExpect(
            jsonPath("$.signature")
                .value(java.util.Base64.getEncoder().encodeToString(new byte[] {1, 2, 3})))
        .andExpect(jsonPath("$.keyId").value("central-server-key"))
        .andExpect(jsonPath("$._links.manifests.href").value("http://localhost/api/v1/manifests"))
        .andExpect(jsonPath("$._links.self.href").exists());
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
    ManifestCreateDTO createDTO = new ManifestCreateDTO(" ", List.of("5f3c9a..."));

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
        new ManifestCreateDTO("Quality Checks 2026-08", List.of("5f3c9a..."));

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
        new ManifestCreateDTO("Quality Checks 2026-08", List.of("5f3c9a..."));

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
        new ManifestCreateDTO("Quality Checks 2026-08", List.of("5f3c9a..."));

    mockMvc
        .perform(
            post(API_V1_MANIFESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
        .andExpect(status().isUnauthorized());
  }
}
