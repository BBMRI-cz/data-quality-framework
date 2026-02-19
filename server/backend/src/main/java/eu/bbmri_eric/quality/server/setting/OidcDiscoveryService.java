package eu.bbmri_eric.quality.server.setting;

/**
 * Service interface for fetching OIDC discovery information from the issuer's well-known endpoint.
 */
public interface OidcDiscoveryService {

  /**
   * Fetches OIDC discovery information from the issuer's well-known endpoint.
   *
   * @param issuerUri the OIDC issuer URI (authority URL)
   * @return OidcDiscoveryDTO containing the discovery information, or null if discovery fails
   */
  OidcDiscoveryDTO fetchDiscoveryDocument(String issuerUri);
}
