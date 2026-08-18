package eu.bbmri_eric.quality.server.crypto;

import java.security.PublicKey;

/** Service for creating and verifying cryptographic signatures. */
public interface SignatureService {

  /**
   * Signs the given data with the configured private key.
   *
   * @param data the data to sign
   * @return the signature
   * @throws Exception if signing fails
   */
  byte[] sign(byte[] data) throws Exception;

  /**
   * Verifies that the given signature is valid for the data using the provided public key.
   *
   * @param data the signed data
   * @param signature the signature to verify
   * @param publicKey the public key used for verification
   * @return {@code true} if the signature is valid, {@code false} otherwise
   * @throws Exception if verification fails
   */
  boolean verify(byte[] data, byte[] signature, PublicKey publicKey) throws Exception;
}
