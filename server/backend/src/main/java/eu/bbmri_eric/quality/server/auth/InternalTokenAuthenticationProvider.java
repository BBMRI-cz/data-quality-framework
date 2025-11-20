package eu.bbmri_eric.quality.server.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

/**
 * AuthenticationProvider for validating internal JWT tokens and creating
 * UsernamePasswordAuthenticationToken.
 */
public class InternalTokenAuthenticationProvider implements AuthenticationProvider {

  private static final Logger logger =
      LoggerFactory.getLogger(InternalTokenAuthenticationProvider.class);

  private final JwtUtil jwtUtil;
  private final UserDetailsService userDetailsService;

  public InternalTokenAuthenticationProvider(
      JwtUtil jwtUtil, UserDetailsService userDetailsService) {
    this.jwtUtil = jwtUtil;
    this.userDetailsService = userDetailsService;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (!(authentication instanceof BearerTokenAuthenticationToken)) {
      logger.warn("Unsupported authentication type: {}", authentication.getClass().getName());
      return null;
    }

    String token = ((BearerTokenAuthenticationToken) authentication).getToken();
    String username;

    try {
      username = jwtUtil.extractUsername(token);
    } catch (Exception e) {
      logger.warn("Failed to parse token: {}", e.getMessage());
      throw new BadCredentialsException("Invalid token format", e);
    }

    if (!jwtUtil.validateToken(token, username)) {
      logger.warn("Token validation failed for user: {}", username);
      throw new BadCredentialsException("Invalid or expired token");
    }

    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    logger.debug("Internal token authentication successful for user: {}", username);

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(userDetails, token, userDetails.getAuthorities());
    auth.setDetails(authentication.getDetails());

    return auth;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
