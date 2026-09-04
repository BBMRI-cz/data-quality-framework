package eu.bbmri_eric.quality.server.crypto.impl;

import eu.bbmri_eric.quality.server.crypto.KeyProvider;
import eu.bbmri_eric.quality.server.crypto.SignatureService;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECKey;
import java.security.spec.ECParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Signs and verifies data using the configured {@link KeyProvider} key. */
@Service
class SignatureServiceImpl implements SignatureService {

  private static final Logger log = LoggerFactory.getLogger(SignatureServiceImpl.class);

  private final KeyProvider keyProvider;
  private final String configuredAlgorithm;

  public SignatureServiceImpl(
      KeyProvider keyProvider,
      @Value("${app.crypto.signature-algorithm:}") String configuredAlgorithm) {
    this.keyProvider = keyProvider;
    this.configuredAlgorithm = configuredAlgorithm;
  }

  @Override
  public byte[] sign(byte[] data) throws GeneralSecurityException {
    PrivateKey privateKey = keyProvider.getPrivateKey();
    String algorithm = resolveAlgorithm(privateKey);
    log.debug(
        "Signing {} bytes using algorithm {} with key {}",
        data.length,
        algorithm,
        privateKey.getAlgorithm());
    try {
      Signature signature = Signature.getInstance(algorithm);
      signature.initSign(privateKey);
      signature.update(data);
      byte[] result = signature.sign();
      log.debug("Created {} byte signature", result.length);
      return result;
    } catch (GeneralSecurityException e) {
      log.warn(
          "Signing failed (algorithm={}, key={}): {}",
          algorithm,
          privateKey.getAlgorithm(),
          e.getMessage(),
          e);
      throw e;
    }
  }

  @Override
  public boolean verify(byte[] data, byte[] signatureBytes, PublicKey publicKey)
      throws GeneralSecurityException {
    String algorithm = resolveAlgorithm(publicKey);
    log.debug(
        "Verifying signature using algorithm {} with key {}", algorithm, publicKey.getAlgorithm());
    try {
      Signature signature = Signature.getInstance(algorithm);
      signature.initVerify(publicKey);
      signature.update(data);
      boolean valid = signature.verify(signatureBytes);
      log.debug("Signature verification result: {}", valid);
      return valid;
    } catch (GeneralSecurityException e) {
      log.warn(
          "Verification failed (algorithm={}, key={}): {}",
          algorithm,
          publicKey.getAlgorithm(),
          e.getMessage(),
          e);
      throw e;
    }
  }

  /**
   * Resolves the signature algorithm to use. Falls back to a sensible default derived from the key
   * type when no algorithm is explicitly configured.
   *
   * @param key the key that will be used for signing or verification
   * @return the signature algorithm name
   */
  private String resolveAlgorithm(Key key) {
    if (configuredAlgorithm != null && !configuredAlgorithm.isBlank()) {
      return configuredAlgorithm;
    }
    return switch (key.getAlgorithm()) {
      case "RSA" -> "SHA256withRSA";
      case "EC" -> ecdsAlgorithm((ECKey) key);
      default ->
          throw new IllegalStateException("Unsupported key algorithm: " + key.getAlgorithm());
    };
  }

  private String ecdsAlgorithm(ECKey key) {
    if (!(key.getParams() instanceof ECParameterSpec params)) {
      return "SHA256withECDSA";
    }
    int bits = params.getCurve().getField().getFieldSize();
    if (bits <= 256) return "SHA256withECDSA";
    if (bits <= 384) return "SHA384withECDSA";
    return "SHA512withECDSA";
  }
}
