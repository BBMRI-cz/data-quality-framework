package eu.bbmri_eric.quality.server.setting;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "OIDC configuration settings for frontend initialization")
public class OidcSettingsDTO {

  @Schema(description = "OIDC authority URL", example = "http://localhost:4011")
  @Pattern(regexp = "^$|^https?://.*", message = "Must be a valid HTTP(S) URL or blank")
  private String oidcAuthority;

  @Schema(description = "OIDC client ID", example = "auth-code-client")
  private String oidcClientId;

  @Schema(description = "OIDC redirect URI", example = "http://localhost:5173/logged-in")
  @Pattern(regexp = "^$|^https?://.*", message = "Must be a valid HTTP(S) URL or blank")
  private String oidcRedirectUri;

  @Schema(description = "OIDC post logout redirect URI", example = "http://localhost:5173")
  @Pattern(regexp = "^$|^https?://.*", message = "Must be a valid HTTP(S) URL or blank")
  private String oidcPostLogoutRedirectUri;

  @Schema(
      description = "OIDC scopes",
      example = "openid profile email permissions some-app-scope-1")
  private String oidcScopes;

  @Schema(description = "Display name for the OIDC authority", example = "BBMRI Identity Provider")
  private String oidcAuthorityName;

  @Schema(description = "OIDC authority logo URL", example = "https://example.test/logo.svg")
  private String oidcAuthorityLogo;

  public OidcSettingsDTO() {}

  public String getOidcAuthority() {
    return oidcAuthority;
  }

  public void setOidcAuthority(String oidcAuthority) {
    this.oidcAuthority = oidcAuthority;
  }

  public String getOidcClientId() {
    return oidcClientId;
  }

  public void setOidcClientId(String oidcClientId) {
    this.oidcClientId = oidcClientId;
  }

  public String getOidcRedirectUri() {
    return oidcRedirectUri;
  }

  public void setOidcRedirectUri(String oidcRedirectUri) {
    this.oidcRedirectUri = oidcRedirectUri;
  }

  public String getOidcPostLogoutRedirectUri() {
    return oidcPostLogoutRedirectUri;
  }

  public void setOidcPostLogoutRedirectUri(String oidcPostLogoutRedirectUri) {
    this.oidcPostLogoutRedirectUri = oidcPostLogoutRedirectUri;
  }

  public String getOidcScopes() {
    return oidcScopes;
  }

  public void setOidcScopes(String oidcScopes) {
    this.oidcScopes = oidcScopes;
  }

  public String getOidcAuthorityName() {
    return oidcAuthorityName;
  }

  public void setOidcAuthorityName(String oidcAuthorityName) {
    this.oidcAuthorityName = oidcAuthorityName;
  }

  public String getOidcAuthorityLogo() {
    return oidcAuthorityLogo;
  }

  public void setOidcAuthorityLogo(String oidcAuthorityLogo) {
    this.oidcAuthorityLogo = oidcAuthorityLogo;
  }

  @Override
  public String toString() {
    return "OidcSettingsDTO{"
        + "oidcAuthority='"
        + oidcAuthority
        + '\''
        + ", oidcClientId='"
        + oidcClientId
        + '\''
        + ", oidcRedirectUri='"
        + oidcRedirectUri
        + '\''
        + ", oidcPostLogoutRedirectUri='"
        + oidcPostLogoutRedirectUri
        + '\''
        + ", oidcScopes='"
        + oidcScopes
        + '\''
        + ", oidcAuthorityName='"
        + oidcAuthorityName
        + '\''
        + ", oidcAuthorityLogo='"
        + oidcAuthorityLogo
        + '\''
        + '}';
  }
}
