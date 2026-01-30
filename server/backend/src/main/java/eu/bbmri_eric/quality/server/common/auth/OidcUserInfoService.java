package eu.bbmri_eric.quality.server.common.auth;

/** Service interface for fetching OIDC user information from the userinfo endpoint. */
interface OidcUserInfoService {

  /**
   * Fetches user information from the OIDC userinfo endpoint using the provided access token.
   *
   * @param accessToken the access token to authenticate the request
   * @return the OidcUserInfo containing user details, or null if fetching fails
   */
  OidcUserInfo fetchUserInfo(String accessToken);
}
