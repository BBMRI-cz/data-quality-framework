package eu.bbmri_eric.quality.agent.server;

import eu.bbmri_eric.quality.agent.server.dto.ManifestVersionDto;

/**
 * Verifies the cryptographic signature of manifest versions published by a central server before
 * their contents are trusted and installed locally.
 */
public interface ManifestSignatureVerifier {

  /**
   * Verifies that the signed body of a manifest version matches its signature under the given
   * PEM-encoded public key.
   *
   * @param publicKeyPem the PEM-encoded public key of the central server
   * @param version the manifest version carrying the signed body, signature and key id
   * @throws ServerCommunicationException if the version is unsigned, no usable key is configured,
   *     or the signature does not match the body
   */
  void verify(String publicKeyPem, ManifestVersionDto version);
}
