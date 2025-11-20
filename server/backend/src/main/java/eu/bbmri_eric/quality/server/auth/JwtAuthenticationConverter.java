package eu.bbmri_eric.quality.server.auth;

import eu.bbmri_eric.quality.server.user.UserDTO;
import eu.bbmri_eric.quality.server.user.UserService;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationConverter.class);

  private final UserService userService;

  public JwtAuthenticationConverter(@Lazy UserService userService) {
    this.userService = userService;
  }

  @Override
  public JwtAuthenticationToken convert(Jwt jwt) {
    String subjectId = jwt.getSubject();
    String principalName = getPrincipalName(jwt);

    logger.debug("Processing OIDC token for subject: {}", subjectId);

    UserDTO user = userService.findOrCreateUserBySubjectId(subjectId, principalName);

    Set<GrantedAuthority> authorities = new HashSet<>();
    user.getRoles()
        .forEach(role -> authorities.add(new SimpleGrantedAuthority(role.getAuthority())));

    logger.debug("OIDC authentication successful for user: {}", user.getUsername());

    return new JwtAuthenticationToken(jwt, authorities, principalName);
  }

  public String getPrincipalName(Jwt jwt) {
    return jwt.getClaimAsString("preferred_username") != null
        ? jwt.getClaimAsString("preferred_username")
        : jwt.getSubject();
  }
}
