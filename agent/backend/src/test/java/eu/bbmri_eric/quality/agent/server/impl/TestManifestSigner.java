package eu.bbmri_eric.quality.agent.server.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.server.dto.ManifestVersionDto;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;

/** Test helper producing a real EC key pair, PEM encodings and signatures for manifest fixtures. */
final class TestManifestSigner {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final KeyPair KEY_PAIR = generateKeyPair();

  private TestManifestSigner() {}

  static KeyPair keyPair() {
    return KEY_PAIR;
  }

  static String publicKeyPem() {
    return toPem(KEY_PAIR);
  }

  static String toPem(KeyPair keyPair) {
    String base64 =
        Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
            .encodeToString(keyPair.getPublic().getEncoded());
    return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
  }

  static String sign(String body) {
    try {
      Signature signature = Signature.getInstance("SHA256withECDSA");
      signature.initSign(KEY_PAIR.getPrivate());
      signature.update(body.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(signature.sign());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  static JsonNode body(String content) {
    try {
      return MAPPER.readTree(content);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Creates a manifest version fixture whose signature matches its body under {@link #keyPair()}.
   */
  static ManifestVersionDto signedVersion(long remoteId, int version) {
    JsonNode body =
        body("{\"manifest_id\":1,\"generated_at\":\"2026-08-13T10:00:00Z\",\"quality_checks\":[]}");
    return new ManifestVersionDto(
        remoteId,
        version,
        Instant.parse("2026-08-13T10:00:00Z"),
        body,
        sign(body.toString()),
        "test-key");
  }

  private static KeyPair generateKeyPair() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
      generator.initialize(new ECGenParameterSpec("secp256r1"));
      return generator.generateKeyPair();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
