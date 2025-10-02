package eu.bbmri_eric.quality.server.user;

/**
 * Data Transfer Object for login responses containing JWT token. Using Java Record for immutable
 * data transfer.
 */
public record LoginResponse(String token, String tokenType, UserDTO user) {
  public LoginResponse(String token, UserDTO user) {
    this(token, "Bearer", user);
  }
}
