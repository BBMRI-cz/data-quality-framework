package eu.bbmri_eric.quality.agent.server.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import eu.bbmri_eric.quality.agent.server.ServerCommunicationException;
import eu.bbmri_eric.quality.agent.server.dto.ManifestVersionDto;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class ManifestSignatureVerifierImplTest {

  private static final Instant GENERATED_AT = Instant.parse("2026-08-13T10:00:00Z");

  private final ManifestSignatureVerifierImpl verifier = new ManifestSignatureVerifierImpl();

  @Test
  void verify_validSignature_passes() {
    JsonNode body = TestManifestSigner.body("{\"manifest_id\":1,\"quality_checks\":[]}");

    assertThatCode(
            () -> verifier.verify(TestManifestSigner.publicKeyPem(), version(body, signOf(body))))
        .doesNotThrowAnyException();
  }

  @Test
  void verify_tamperedBody_throwsServerCommunicationException() {
    JsonNode signedBody = TestManifestSigner.body("{\"manifest_id\":1}");
    String signature = TestManifestSigner.sign(signedBody.toString());
    ManifestVersionDto tampered =
        version(TestManifestSigner.body("{\"manifest_id\":2}"), signature);

    assertThatThrownBy(() -> verifier.verify(TestManifestSigner.publicKeyPem(), tampered))
        .isInstanceOf(ServerCommunicationException.class)
        .hasMessageContaining("invalid signature");
  }

  @Test
  void verify_missingSignature_throwsServerCommunicationException() {
    ManifestVersionDto unsigned = version(TestManifestSigner.body("{\"manifest_id\":1}"), null);

    assertThatThrownBy(() -> verifier.verify(TestManifestSigner.publicKeyPem(), unsigned))
        .isInstanceOf(ServerCommunicationException.class)
        .hasMessageContaining("unsigned");
  }

  @Test
  void verify_missingKeyId_throwsServerCommunicationException() {
    JsonNode body = TestManifestSigner.body("{\"manifest_id\":1}");
    ManifestVersionDto version =
        new ManifestVersionDto(
            1L, 2, GENERATED_AT, body, TestManifestSigner.sign(body.toString()), null);

    assertThatThrownBy(() -> verifier.verify(TestManifestSigner.publicKeyPem(), version))
        .isInstanceOf(ServerCommunicationException.class)
        .hasMessageContaining("unsigned");
  }

  @Test
  void verify_noPublicKey_throwsServerCommunicationException() {
    JsonNode body = TestManifestSigner.body("{\"manifest_id\":1}");

    assertThatThrownBy(() -> verifier.verify(" ", version(body, signOf(body))))
        .isInstanceOf(ServerCommunicationException.class)
        .hasMessageContaining("no public key");
  }

  @Test
  void verify_malformedPublicKey_throwsServerCommunicationException() {
    JsonNode body = TestManifestSigner.body("{\"manifest_id\":1}");

    assertThatThrownBy(() -> verifier.verify("not-a-pem", version(body, signOf(body))))
        .isInstanceOf(ServerCommunicationException.class);
  }

  @Test
  void verify_malformedSignature_throwsServerCommunicationException() {
    JsonNode body = TestManifestSigner.body("{\"manifest_id\":1}");
    ManifestVersionDto version = version(body, "!!!not-base64!!!");

    assertThatThrownBy(() -> verifier.verify(TestManifestSigner.publicKeyPem(), version))
        .isInstanceOf(ServerCommunicationException.class)
        .hasMessageContaining("Base64");
  }

  @Test
  void verify_rsaKeySignature_passes() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair keyPair = generator.generateKeyPair();
    JsonNode body = TestManifestSigner.body("{\"manifest_id\":1}");
    Signature signature = Signature.getInstance("SHA256withRSA");
    signature.initSign(keyPair.getPrivate());
    signature.update(body.toString().getBytes(StandardCharsets.UTF_8));
    String rsaSignature = Base64.getEncoder().encodeToString(signature.sign());

    assertThatCode(
            () -> verifier.verify(TestManifestSigner.toPem(keyPair), version(body, rsaSignature)))
        .doesNotThrowAnyException();
  }

  private String signOf(JsonNode body) {
    return TestManifestSigner.sign(body.toString());
  }

  private ManifestVersionDto version(JsonNode body, String signature) {
    return new ManifestVersionDto(1L, 2, GENERATED_AT, body, signature, "test-key");
  }
}
