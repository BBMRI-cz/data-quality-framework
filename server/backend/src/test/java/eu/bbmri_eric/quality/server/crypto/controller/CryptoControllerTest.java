package eu.bbmri_eric.quality.server.crypto.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.crypto.KeyProvider;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CryptoControllerTest {

  public static final String API_V1_PUBLIC_KEY = "/api/v1/public-key";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private KeyProvider keyProvider;

  private static PublicKey createPublicKey() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair keyPair = generator.generateKeyPair();
    return keyPair.getPublic();
  }

  @Test
  void getPublicKey_withoutAuthentication_shouldReturnPublicKey() throws Exception {
    given(keyProvider.getKeyId()).willReturn("central-signing");
    given(keyProvider.getPublicKey()).willReturn(createPublicKey());

    mockMvc
        .perform(get(API_V1_PUBLIC_KEY))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.keyId").value("central-signing"))
        .andExpect(jsonPath("$.publicKey").isNotEmpty());
  }

  @Test
  void getPublicKey_withoutAuthentication_whenNotConfigured_shouldReturn404() throws Exception {
    given(keyProvider.getPublicKey())
        .willThrow(new EntityNotFoundException("Cryptographic functionality is not setup"));

    mockMvc.perform(get(API_V1_PUBLIC_KEY)).andExpect(status().isNotFound());
  }
}
