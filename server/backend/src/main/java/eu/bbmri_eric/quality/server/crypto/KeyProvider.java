package eu.bbmri_eric.quality.server.crypto;

import java.security.PrivateKey;
import java.security.PublicKey;

/** Provides the cryptographic keys used by the server for signing and verifying signatures. */
public interface KeyProvider {

  /**
   * Returns the private key used for signing.
   *
   * @return the private key
   */
  PrivateKey getPrivateKey();

  /**
   * Returns the public key used for verification.
   *
   * @return the public key
   */
  PublicKey getPublicKey();

  /**
   * Returns the identifier of the key.
   *
   * @return the key id
   */
  String getKeyId();
}
