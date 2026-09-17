package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.server.ManifestSignatureVerifier;
import eu.bbmri_eric.quality.agent.server.ServerCommunicationException;
import eu.bbmri_eric.quality.agent.server.dto.ManifestVersionDto;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link ManifestSignatureVerifier}. Mirrors the central server's signing
 * scheme: the compact JSON body is signed with an EC or RSA key and the signature is Base64
 * encoded.
 */
@Component
class ManifestSignatureVerifierImpl implements ManifestSignatureVerifier {

  private static final Logger log = LoggerFactory.getLogger(ManifestSignatureVerifierImpl.class);
  private static final List<String> SUPPORTED_KEY_ALGORITHMS = List.of("EC", "RSA");

  @Override
  public void verify(String publicKeyPem, ManifestVersionDto version) {
    requireSignedVersion(version);
    PublicKey publicKey = parsePublicKey(publicKeyPem);
    byte[] signature = decodeSignature(version.getSignature());
    if (!signatureMatches(
        publicKey, version.getBody().toString().getBytes(StandardCharsets.UTF_8), signature)) {
      log.warn(
          "Rejected manifest version {} (keyId={}): signature does not match the body",
          version.getVersion(),
          version.getKeyId());
      throw new ServerCommunicationException(
          "Manifest version %d has an invalid signature and was rejected"
              .formatted(version.getVersion()));
    }
  }

  private void requireSignedVersion(ManifestVersionDto version) {
    if (version.getBody() == null
        || version.getSignature() == null
        || version.getSignature().isBlank()
        || version.getKeyId() == null
        || version.getKeyId().isBlank()) {
      throw new ServerCommunicationException(
          "Manifest version %d is unsigned and cannot be downloaded"
              .formatted(version.getVersion()));
    }
  }

  private boolean signatureMatches(PublicKey publicKey, byte[] body, byte[] signatureBytes) {
    try {
      Signature verifier = Signature.getInstance(resolveAlgorithm(publicKey));
      verifier.initVerify(publicKey);
      verifier.update(body);
      return verifier.verify(signatureBytes);
    } catch (GeneralSecurityException e) {
      throw new ServerCommunicationException("Failed to verify the manifest signature", e);
    }
  }

  private PublicKey parsePublicKey(String pem) {
    if (pem == null || pem.isBlank()) {
      throw new ServerCommunicationException(
          "Cannot verify the manifest signature because no public key is configured for the"
              + " server");
    }
    byte[] der;
    try {
      String base64 =
          pem.replaceAll("-----BEGIN [A-Z0-9 ]*-----", "")
              .replaceAll("-----END [A-Z0-9 ]*-----", "")
              .replaceAll("\\s", "");
      der = Base64.getDecoder().decode(base64);
    } catch (IllegalArgumentException e) {
      throw new ServerCommunicationException("The configured public key is not valid PEM", e);
    }
    X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
    for (String algorithm : SUPPORTED_KEY_ALGORITHMS) {
      try {
        return KeyFactory.getInstance(algorithm).generatePublic(spec);
      } catch (InvalidKeySpecException | NoSuchAlgorithmException ignored) {
        // The PEM header does not reveal the key type; try the next supported algorithm.
      }
    }
    throw new ServerCommunicationException(
        "The configured public key is not a valid EC or RSA public key");
  }

  private byte[] decodeSignature(String signature) {
    try {
      return Base64.getDecoder().decode(signature);
    } catch (IllegalArgumentException e) {
      throw new ServerCommunicationException("The manifest signature is not valid Base64", e);
    }
  }

  /**
   * Resolves the signature algorithm the same way the central server does when signing, based on
   * the key type and EC curve size.
   *
   * @param key the public key
   * @return the signature algorithm name
   */
  private String resolveAlgorithm(PublicKey key) {
    return switch (key.getAlgorithm()) {
      case "RSA" -> "SHA256withRSA";
      case "EC" -> ecdsaAlgorithm((ECKey) key);
      default ->
          throw new ServerCommunicationException(
              "Unsupported public key algorithm: " + key.getAlgorithm());
    };
  }

  private String ecdsaAlgorithm(ECKey key) {
    if (!(key.getParams() instanceof ECParameterSpec params)) {
      return "SHA256withECDSA";
    }
    int bits = params.getCurve().getField().getFieldSize();
    if (bits <= 256) {
      return "SHA256withECDSA";
    }
    if (bits <= 384) {
      return "SHA384withECDSA";
    }
    return "SHA512withECDSA";
  }
}
