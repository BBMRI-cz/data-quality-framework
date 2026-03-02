package eu.bbmri_eric.quality.server.setting;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing the OIDC discovery document from the well-known endpoint. Contains essential
 * endpoints for OAuth2/OIDC flows.
 */
@Schema(description = "OIDC discovery document containing OAuth2/OIDC endpoints")
public class OidcDiscoveryDTO {

  @Schema(
      description = "Authorization endpoint URL",
      example = "http://localhost:4011/connect/authorize")
  @JsonProperty("authorization_endpoint")
  private String authorizationEndpoint;

  @Schema(description = "Token endpoint URL", example = "http://localhost:4011/connect/token")
  @JsonProperty("token_endpoint")
  private String tokenEndpoint;

  @Schema(
      description = "End session endpoint URL",
      example = "http://localhost:4011/connect/endsession")
  @JsonProperty("end_session_endpoint")
  private String endSessionEndpoint;

  @Schema(description = "Issuer URL", example = "http://localhost:4011")
  @JsonProperty("issuer")
  private String issuer;

  @Schema(
      description = "JWKS URI",
      example = "http://localhost:4011/.well-known/openid-configuration/jwks")
  @JsonProperty("jwks_uri")
  private String jwksUri;

  @Schema(description = "UserInfo endpoint URL", example = "http://localhost:4011/connect/userinfo")
  @JsonProperty("userinfo_endpoint")
  private String userInfoEndpoint;

  public OidcDiscoveryDTO() {}

  public String getAuthorizationEndpoint() {
    return authorizationEndpoint;
  }

  public void setAuthorizationEndpoint(String authorizationEndpoint) {
    this.authorizationEndpoint = authorizationEndpoint;
  }

  public String getTokenEndpoint() {
    return tokenEndpoint;
  }

  public void setTokenEndpoint(String tokenEndpoint) {
    this.tokenEndpoint = tokenEndpoint;
  }

  public String getEndSessionEndpoint() {
    return endSessionEndpoint;
  }

  public void setEndSessionEndpoint(String endSessionEndpoint) {
    this.endSessionEndpoint = endSessionEndpoint;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getJwksUri() {
    return jwksUri;
  }

  public void setJwksUri(String jwksUri) {
    this.jwksUri = jwksUri;
  }

  public String getUserInfoEndpoint() {
    return userInfoEndpoint;
  }

  public void setUserInfoEndpoint(String userInfoEndpoint) {
    this.userInfoEndpoint = userInfoEndpoint;
  }

  @Override
  public String toString() {
    return "OidcDiscoveryDTO{"
        + "authorizationEndpoint='"
        + authorizationEndpoint
        + '\''
        + ", tokenEndpoint='"
        + tokenEndpoint
        + '\''
        + ", endSessionEndpoint='"
        + endSessionEndpoint
        + '\''
        + ", issuer='"
        + issuer
        + '\''
        + ", jwksUri='"
        + jwksUri
        + '\''
        + ", userInfoEndpoint='"
        + userInfoEndpoint
        + '\''
        + '}';
  }
}
