package eu.bbmri_eric.quality.server.crypto.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.quality.server.crypto.KeyProvider;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.junit.jupiter.api.Test;

class SignatureServiceImplTest {

  private final KeyPair keyPair = generateRsaKeyPair();
  private final SignatureServiceImpl signatureService =
      new SignatureServiceImpl(new TestKeyProvider(keyPair), "SHA256withRSA");

  @Test
  void signAndVerify_shouldRoundTrip() throws Exception {
    byte[] data = "payload".getBytes(StandardCharsets.UTF_8);

    byte[] signature = signatureService.sign(data);

    assertTrue(signatureService.verify(data, signature, keyPair.getPublic()));
  }

  @Test
  void verify_shouldReturnFalseForTamperedData() throws Exception {
    byte[] data = "payload".getBytes(StandardCharsets.UTF_8);
    byte[] signature = signatureService.sign(data);

    assertFalse(
        signatureService.verify(
            "tampered".getBytes(StandardCharsets.UTF_8), signature, keyPair.getPublic()));
  }

  @Test
  void sign_shouldFailWhenNoKeyIsConfigured() {
    SignatureServiceImpl unconfigured =
        new SignatureServiceImpl(new MissingKeyProvider(), "SHA256withRSA");

    assertThrows(
        GeneralSecurityException.class,
        () -> unconfigured.sign("payload".getBytes(StandardCharsets.UTF_8)));
  }

  private static KeyPair generateRsaKeyPair() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  private static final class TestKeyProvider implements KeyProvider {
    private final KeyPair keyPair;

    private TestKeyProvider(KeyPair keyPair) {
      this.keyPair = keyPair;
    }

    @Override
    public PrivateKey getPrivateKey() {
      return keyPair.getPrivate();
    }

    @Override
    public PublicKey getPublicKey() {
      return keyPair.getPublic();
    }

    @Override
    public String getKeyId() {
      return "test-key";
    }
  }

  private static final class MissingKeyProvider implements KeyProvider {
    @Override
    public PrivateKey getPrivateKey() {
      return null;
    }

    @Override
    public PublicKey getPublicKey() {
      return null;
    }

    @Override
    public String getKeyId() {
      return null;
    }
  }
}
