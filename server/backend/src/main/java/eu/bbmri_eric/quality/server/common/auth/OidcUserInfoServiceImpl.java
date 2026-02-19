package eu.bbmri_eric.quality.server.common.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import eu.bbmri_eric.quality.server.setting.OidcIssuerProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/** Service to fetch and cache OIDC user information from the userinfo endpoint. */
@Service
class OidcUserInfoServiceImpl implements OidcUserInfoService {

  private static final Logger logger = LoggerFactory.getLogger(OidcUserInfoServiceImpl.class);
  private static final Duration CACHE_DURATION = Duration.ofMinutes(15);
  private static final long MAX_CACHE_SIZE = 10_000;
  private static final String OIDC_DISCOVERY_PATH = "/.well-known/openid-configuration";

  private final Cache<String, OidcUserInfo> cache;
  private final RestTemplate restTemplate;
  private final OidcIssuerProvider oidcIssuerProvider;
  private volatile String userInfoEndpoint;
  private volatile boolean initialized = false;

  public OidcUserInfoServiceImpl(OidcIssuerProvider oidcIssuerProvider) {
    this.restTemplate = new RestTemplate();
    this.oidcIssuerProvider = oidcIssuerProvider;
    this.cache =
        Caffeine.newBuilder().expireAfterWrite(CACHE_DURATION).maximumSize(MAX_CACHE_SIZE).build();
  }

  private void initializeUserInfoEndpointIfNeeded() {
    if (initialized) {
      return;
    }

    synchronized (this) {
      if (initialized) {
        return;
      }
      initialized = true;

      try {
        String issuerUri = oidcIssuerProvider.getIssuerUri();
        if (issuerUri == null || issuerUri.isBlank()) {
          logger.info("OIDC issuer URI not configured, userinfo endpoint will not be available");
          userInfoEndpoint = null;
          return;
        }

        var discoveryUri = issuerUri + OIDC_DISCOVERY_PATH;
        var discoveryResponse = fetchDiscoveryDocument(discoveryUri);
        userInfoEndpoint = extractUserInfoEndpoint(discoveryResponse, discoveryUri);
      } catch (Exception e) {
        logger.error(
            "Failed to fetch OIDC discovery document, userinfo endpoint will not be available: {}",
            e.getMessage());
        logger.debug("Full error details:", e);
        userInfoEndpoint = null;
      }
    }
  }

  private OidcDiscoveryResponse fetchDiscoveryDocument(String discoveryUri) {
    logger.debug("Fetching OIDC discovery document from: {}", discoveryUri);
    return restTemplate.getForObject(discoveryUri, OidcDiscoveryResponse.class);
  }

  private String extractUserInfoEndpoint(
      OidcDiscoveryResponse discoveryResponse, String discoveryUri) {
    if (discoveryResponse == null || discoveryResponse.userInfoEndpoint() == null) {
      logger.error(
          "Invalid OIDC discovery document from {}, missing userinfo_endpoint", discoveryUri);
      return null;
    }
    var endpoint = discoveryResponse.userInfoEndpoint();
    logger.info("OIDC userinfo endpoint configured: {}", endpoint);
    return endpoint;
  }

  @Override
  public OidcUserInfo fetchUserInfo(String accessToken) {
    initializeUserInfoEndpointIfNeeded();

    if (!canFetchUserInfo(accessToken)) {
      return null;
    }
    var cacheKey = hashToken(accessToken);
    var cached = cache.getIfPresent(cacheKey);
    if (cached != null) {
      return cached;
    }
    var userInfo = fetchRemoteUserInfo(accessToken);
    if (userInfo != null) {
      cache.put(cacheKey, userInfo);
    }
    return userInfo;
  }

  private boolean canFetchUserInfo(String accessToken) {
    if (userInfoEndpoint == null) {
      logger.warn("OIDC userinfo endpoint not available, skipping userinfo fetch");
      return false;
    }
    if (accessToken == null || accessToken.isBlank()) {
      logger.warn("Access token is null or blank, cannot fetch userinfo");
      return false;
    }
    return true;
  }

  private OidcUserInfo fetchRemoteUserInfo(String accessToken) {
    try {
      var headers = new HttpHeaders();
      headers.setBearerAuth(accessToken);
      var request = new HttpEntity<>(headers);

      ResponseEntity<OidcUserInfo> response =
          restTemplate.exchange(userInfoEndpoint, HttpMethod.GET, request, OidcUserInfo.class);

      var userInfo = response.getBody();
      if (userInfo == null) {
        logger.warn("UserInfo response body is null");
      }
      return userInfo;
    } catch (Exception e) {
      logger.error("Failed to fetch userinfo from {}: {}", userInfoEndpoint, e.getMessage(), e);
      return null;
    }
  }

  /**
   * Hashes the access token using SHA-256 to create a secure cache key. This prevents the actual
   * token from being stored in memory as a map key, reducing the attack surface.
   *
   * @param token the access token to hash
   * @return SHA-256 hash of the token as a hex string
   */
  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      logger.error("SHA-256 algorithm not available", e);
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record OidcDiscoveryResponse(
      @JsonProperty("userinfo_endpoint") String userInfoEndpoint) {}
}
