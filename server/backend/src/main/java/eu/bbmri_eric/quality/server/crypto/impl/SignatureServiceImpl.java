package eu.bbmri_eric.quality.server.crypto.impl;

import eu.bbmri_eric.quality.server.crypto.KeyProvider;
import eu.bbmri_eric.quality.server.crypto.SignatureService;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Signs and verifies data using the configured {@link KeyProvider} key. */
@Service
class SignatureServiceImpl implements SignatureService {

  private final KeyProvider keyProvider;
  private final String algorithm;

  public SignatureServiceImpl(
      KeyProvider keyProvider,
      @Value("${app.crypto.signature-algorithm:SHA256withRSA}") String algorithm) {
    this.keyProvider = keyProvider;
    this.algorithm = algorithm;
  }

  @Override
  public byte[] sign(byte[] data) throws GeneralSecurityException {
    Signature signature = Signature.getInstance(algorithm);
    signature.initSign(keyProvider.getPrivateKey());
    signature.update(data);
    return signature.sign();
  }

  @Override
  public boolean verify(byte[] data, byte[] signatureBytes, PublicKey publicKey)
      throws GeneralSecurityException {
    Signature signature = Signature.getInstance(algorithm);
    signature.initVerify(publicKey);
    signature.update(data);
    return signature.verify(signatureBytes);
  }
}
