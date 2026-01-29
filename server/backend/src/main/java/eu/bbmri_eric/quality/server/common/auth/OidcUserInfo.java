package eu.bbmri_eric.quality.server.common.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

/** DTO representing user information from the OIDC userinfo endpoint. */
record OidcUserInfo(
    @JsonProperty("sub") String subject,
    @JsonProperty("preferred_username") String preferredUsername,
    @JsonProperty("name") String name,
    @JsonProperty("email") String email,
    @JsonProperty("given_name") String givenName,
    @JsonProperty("family_name") String familyName) {

  /**
   * Returns the Name from the user info, or falls back to the subject if Name is not available.
   *
   * @return username
   */
  public String getFullName() {
    if (name != null && !name.isBlank()) {
      return name;
    }
    return subject;
  }
}
