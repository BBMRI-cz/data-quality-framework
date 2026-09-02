package eu.bbmri_eric.quality.server.crypto;

/** Thrown when a cryptographic signature cannot be created. */
public class SignatureException extends RuntimeException {

  public SignatureException(String message, Throwable cause) {
    super(message, cause);
  }
}
