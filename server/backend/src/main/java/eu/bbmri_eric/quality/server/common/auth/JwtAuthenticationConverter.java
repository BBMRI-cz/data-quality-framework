package eu.bbmri_eric.quality.server.common.auth;

import eu.bbmri_eric.quality.server.user.UserDTO;
import eu.bbmri_eric.quality.server.user.UserNotFoundException;
import eu.bbmri_eric.quality.server.user.UserService;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Converts a Jwt token into a JwtAuthenticationToken by validating claims, retrieving or creating
 * the associated user, and extracting authorities.
 */
@Component
class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationConverter.class);

  private enum OAuth2FlowType {
    CLIENT_CREDENTIALS,
    AUTHORIZATION_CODE
  }

  private final UserService userService;
  private final JwtValidator jwtValidator;
  private final OidcUserInfoService oidcUserInfoService;

  public JwtAuthenticationConverter(
      @Lazy UserService userService,
      JwtValidator jwtValidator,
      OidcUserInfoService oidcUserInfoService) {
    this.userService = userService;
    this.jwtValidator = jwtValidator;
    this.oidcUserInfoService = oidcUserInfoService;
  }

  /**
   * Internal record to hold identity information extracted from JWT.
   *
   * @param identityId the unique identifier (sub for users, client_id for clients)
   * @param username the display name or identifier to use
   */
  private record TokenIdentity(String identityId, String username) {}

  /**
   * Converts the given Jwt into a JwtAuthenticationToken after validation and user retrieval.
   * Supports both authorization code flow (user authentication) and client-credentials flow
   * (service/client authentication).
   *
   * @param jwt the JWT token to convert
   * @return JwtAuthenticationToken representing the authenticated user or client
   * @throws IllegalArgumentException if JWT validation fails or required claims are missing
   */
  @Override
  public JwtAuthenticationToken convert(@NonNull Jwt jwt) {
    Objects.requireNonNull(jwt, "JWT cannot be null");

    jwtValidator.validate(jwt);

    TokenIdentity identity = extractIdentity(jwt);
    UserDTO user = findOrCreateUser(identity);
    Set<GrantedAuthority> authorities = extractAuthorities(user);

    return new JwtAuthenticationToken(jwt, authorities, identity.username());
  }

  /**
   * Extracts identity information from JWT based on the OAuth2 flow type.
   *
   * @param jwt the JWT token
   * @return TokenIdentity containing identityId and username
   * @throws IllegalArgumentException if required claims are missing
   */
  private TokenIdentity extractIdentity(Jwt jwt) {
    OAuth2FlowType flowType = determineFlowType(jwt);
    return switch (flowType) {
      case CLIENT_CREDENTIALS -> extractClientIdentity(jwt);
      case AUTHORIZATION_CODE -> extractUserIdentity(jwt);
    };
  }

  /**
   * Extracts identity for client-credentials flow.
   *
   * @param jwt the JWT token
   * @return TokenIdentity with client_id as both identityId and username
   */
  private TokenIdentity extractClientIdentity(Jwt jwt) {
    String clientId = jwt.getClaimAsString("client_id");
    logger.debug("Processing client-credentials token for client: {}", clientId);
    return new TokenIdentity(clientId, clientId);
  }

  /**
   * Extracts identity for authorization code flow and username from userinfo endpoint.
   *
   * @param jwt the JWT token
   * @return TokenIdentity with sub as identityId and extracted username
   * @throws IllegalArgumentException if sub claim is missing or blank
   */
  private TokenIdentity extractUserIdentity(Jwt jwt) {
    String subjectId = jwt.getSubject();
    if (subjectId == null || subjectId.isBlank()) {
      throw new IllegalArgumentException(
          "JWT from authorization code flow must contain 'sub' claim");
    }
    String userName = extractUsername(jwt);
    logger.debug("Processing authorization code token for user: {} (sub: {})", userName, subjectId);
    return new TokenIdentity(subjectId, userName);
  }

  /**
   * Finds an existing user by subject ID or creates a new one if not found.
   *
   * @param identity the token identity containing subject ID and username
   * @return UserDTO representing the user
   */
  private UserDTO findOrCreateUser(TokenIdentity identity) {
    try {
      return userService.findBySubjectId(identity.identityId());
    } catch (UserNotFoundException e) {
      logger.debug("User not found for subject ID: {}, creating new user", identity.identityId());
      try{
        // To catch concurrent writes
        return userService.createBySubjectId(identity.identityId(), identity.username());
      } catch (JpaSystemException ex){
        return userService.findBySubjectId(identity.identityId());
      }
    }
  }

  /**
   * Extracts username from JWT, preferring userinfo endpoint data (with 15-min cache), then JWT
   * claims ('preferred_username'), and finally 'sub' as fallback.
   *
   * @param jwt the JWT token
   * @return username (never null or blank)
   */
  private String extractUsername(Jwt jwt) {
    String accessToken = jwt.getTokenValue();
    String subjectId = jwt.getSubject();

    OidcUserInfo userInfo = oidcUserInfoService.fetchUserInfo(accessToken);

    if (userInfo != null) {
      String username = userInfo.getFullName();
      if (username != null && !username.isBlank()) {
        try {
          userService.updateUsername(subjectId, username);
        } catch (UserNotFoundException e) {
          logger.debug("User not yet created for subject {}, will be created later", subjectId);
        }
        return username;
      }
    }
    return subjectId;
  }

  /**
   * Extracts authorities from user roles as an immutable set.
   *
   * @param user the user DTO
   * @return immutable set of granted authorities
   */
  private Set<GrantedAuthority> extractAuthorities(UserDTO user) {
    if (user.getRoles() == null || user.getRoles().isEmpty()) {
      logger.warn("User {} has no roles assigned", user.getUsername());
      return Collections.emptySet();
    }

    return user.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Determines the OAuth2 flow type based on JWT claims.
   *
   * @param jwt the JWT token
   * @return OAuth2FlowType (CLIENT_CREDENTIALS or AUTHORIZATION_CODE)
   */
  private OAuth2FlowType determineFlowType(Jwt jwt) {
    String clientId = jwt.getClaimAsString("client_id");
    String scope = jwt.getClaimAsString("scope");
    boolean isClientCredentials = clientId != null && scope != null && !scope.contains("openid");
    return isClientCredentials
        ? OAuth2FlowType.CLIENT_CREDENTIALS
        : OAuth2FlowType.AUTHORIZATION_CODE;
  }
}
