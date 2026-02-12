package eu.bbmri_eric.quality.server.setting;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class OidcDiscoveryServiceImpl implements OidcDiscoveryService {

  private static final Logger log = LoggerFactory.getLogger(OidcDiscoveryServiceImpl.class);
  private static final String WELL_KNOWN_PATH = "/.well-known/openid-configuration";
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

  private final String issuerUri;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  OidcDiscoveryServiceImpl(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:#{null}}") String issuerUri,
      ObjectMapper objectMapper) {
    this.issuerUri = issuerUri;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
  }

  @Override
  public OidcDiscoveryDTO fetchDiscoveryDocument() {
    if (issuerUri == null || issuerUri.isBlank()) {
      log.warn("OIDC issuer URI is not configured");
      return null;
    }

    var wellKnownUrl = issuerUri + WELL_KNOWN_PATH;
    log.debug("Fetching OIDC discovery document from: {}", wellKnownUrl);

    try {
      var request =
          HttpRequest.newBuilder()
              .uri(URI.create(wellKnownUrl))
              .timeout(REQUEST_TIMEOUT)
              .GET()
              .build();

      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        log.warn(
            "OIDC discovery endpoint returned HTTP {}: {}", response.statusCode(), wellKnownUrl);
        return null;
      }

      var discoveryDTO = objectMapper.readValue(response.body(), OidcDiscoveryDTO.class);
      log.info("Successfully fetched OIDC discovery document from {}", wellKnownUrl);
      return discoveryDTO;

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("OIDC discovery request was interrupted");
      return null;
    } catch (Exception e) {
      log.warn("OIDC server unavailable at {}: {}", wellKnownUrl, e.getMessage());
      return null;
    }
  }
}
