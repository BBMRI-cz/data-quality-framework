package eu.bbmri_eric.quality.server.common.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
  private static final String OIDC_DISCOVERY_PATH = "/.well-known/openid-configuration";
  private static final String USERINFO_ENDPOINT_KEY = "userinfo_endpoint";

  private final Cache<String, OidcUserInfo> cache;
  private final RestTemplate restTemplate;
  private String userInfoEndpoint;
  private final String issuerUri;

  public OidcUserInfoServiceImpl(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:#{null}}") String issuerUri) {
    this.restTemplate = new RestTemplate();
    this.issuerUri = issuerUri;
    this.cache = Caffeine.newBuilder().expireAfterWrite(CACHE_DURATION).maximumSize(10_000).build();
  }

  @PostConstruct
  private void init() {
    if (issuerUri == null || issuerUri.isBlank()) {
      logger.info("OIDC issuer URI not configured, userinfo endpoint will not be available");
      return;
    }

    try {
      var discoveryUri = issuerUri + OIDC_DISCOVERY_PATH;
      logger.debug("Fetching OIDC discovery document from: {}", discoveryUri);

      Map<String, Object> discoveryResult = restTemplate.getForObject(discoveryUri, Map.class);

      if (discoveryResult == null || !discoveryResult.containsKey(USERINFO_ENDPOINT_KEY)) {
        logger.error(
            "Invalid OIDC discovery document from {}, missing {}",
            discoveryUri,
            USERINFO_ENDPOINT_KEY);
        this.userInfoEndpoint = null;
        return;
      }

      this.userInfoEndpoint = (String) discoveryResult.get(USERINFO_ENDPOINT_KEY);
      logger.info("OIDC userinfo endpoint configured: {}", userInfoEndpoint);
    } catch (Exception e) {
      logger.error(
          "Failed to fetch OIDC discovery document from issuer: {}, userinfo endpoint will not be available",
          issuerUri,
          e);
      this.userInfoEndpoint = null;
    }
  }

  @Override
  public OidcUserInfo fetchUserInfo(String accessToken) {
    if (userInfoEndpoint == null) {
      logger.warn("OIDC issuer URI not configured, skipping userinfo fetch");
      return null;
    }

    if (accessToken == null || accessToken.isBlank()) {
      logger.warn("Access token is null or blank, cannot fetch userinfo");
      return null;
    }

    var cacheKey = hashToken(accessToken);
    var cached = cache.getIfPresent(cacheKey);
    if (cached != null) {
      return cached;
    }

    try {
      var headers = new HttpHeaders();
      headers.setBearerAuth(accessToken);

      var request = new HttpEntity<>(headers);

      ResponseEntity<OidcUserInfo> response =
          restTemplate.exchange(userInfoEndpoint, HttpMethod.GET, request, OidcUserInfo.class);

      var userInfo = response.getBody();
      if (userInfo != null) {
        cache.put(cacheKey, userInfo);
      } else {
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
}
