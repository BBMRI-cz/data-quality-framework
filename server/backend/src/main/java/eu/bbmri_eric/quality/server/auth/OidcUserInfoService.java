package eu.bbmri_eric.quality.server.auth;

import eu.bbmri_eric.quality.server.auth.dto.OidcUserInfo;

/** Service interface for fetching OIDC user information from the userinfo endpoint. */
public interface OidcUserInfoService {

  /**
   * Fetches user information from the OIDC userinfo endpoint using the provided access token.
   *
   * @param accessToken the access token to authenticate the request
   * @return the OidcUserInfo containing user details or null if fetching fails
   */
  OidcUserInfo fetchUserInfo(String accessToken);
}
