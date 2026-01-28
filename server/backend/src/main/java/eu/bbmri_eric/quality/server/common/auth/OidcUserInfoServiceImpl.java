package eu.bbmri_eric.quality.server.common.auth;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

  private final Map<String, CachedUserInfo> cache = new ConcurrentHashMap<>();
  private final RestTemplate restTemplate;
  private final String userInfoEndpoint;

  public OidcUserInfoServiceImpl(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:#{null}}") String issuerUri) {
    this.restTemplate = new RestTemplate();
    this.userInfoEndpoint = issuerUri != null ? issuerUri + "/connect/userinfo" : null;
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

    var cached = cache.get(accessToken);
    if (cached != null && !cached.isExpired()) {
      return cached.userInfo();
    }

    try {
      var headers = new HttpHeaders();
      headers.setBearerAuth(accessToken);

      var request = new HttpEntity<>(headers);

      ResponseEntity<OidcUserInfo> response =
          restTemplate.exchange(userInfoEndpoint, HttpMethod.GET, request, OidcUserInfo.class);

      var userInfo = response.getBody();
      if (userInfo != null) {
        cache.put(accessToken, new CachedUserInfo(userInfo, System.currentTimeMillis()));
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
   * Internal record to hold cached userinfo with timestamp.
   *
   * @param userInfo the OIDC user information
   * @param timestamp the time when this entry was cached
   */
  private record CachedUserInfo(OidcUserInfo userInfo, long timestamp) {

    /**
     * Checks if this cache entry has expired.
     *
     * @return true if expired, false otherwise
     */
    boolean isExpired() {
      return System.currentTimeMillis() - timestamp > CACHE_DURATION.toMillis();
    }
  }
}
