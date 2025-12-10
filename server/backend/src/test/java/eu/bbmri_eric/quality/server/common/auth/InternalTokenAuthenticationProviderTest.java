package eu.bbmri_eric.quality.server.common.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.JwtException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

/** Unit tests for InternalTokenAuthenticationProvider. */
@ExtendWith(MockitoExtension.class)
class InternalTokenAuthenticationProviderTest {

  @Mock private JwtUtil jwtUtil;

  @Mock private UserDetailsService userDetailsService;

  private InternalTokenAuthenticationProvider provider;

  private UserDetails userDetails;

  @BeforeEach
  void setUp() {
    provider = new InternalTokenAuthenticationProvider(jwtUtil, userDetailsService);

    userDetails =
        User.builder()
            .username("testuser")
            .password("password")
            .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
            .build();
  }

  @Test
  @DisplayName("Should authenticate valid internal token successfully")
  void authenticate_withValidToken_returnsAuthentication() {
    String token = "valid.jwt.token";
    BearerTokenAuthenticationToken authRequest = new BearerTokenAuthenticationToken(token);

    when(jwtUtil.extractUsername(token)).thenReturn("testuser");
    when(jwtUtil.validateToken(token, "testuser")).thenReturn(true);
    when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

    Authentication result = provider.authenticate(authRequest);

    assertNotNull(result);
    assertInstanceOf(UsernamePasswordAuthenticationToken.class, result);
    assertEquals(userDetails, result.getPrincipal());
    assertEquals(token, result.getCredentials());
    assertEquals(1, result.getAuthorities().size());
    assertTrue(
        result.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

    verify(jwtUtil).extractUsername(token);
    verify(jwtUtil).validateToken(token, "testuser");
    verify(userDetailsService).loadUserByUsername("testuser");
  }

  @Test
  @DisplayName("Should throw BadCredentialsException when token parsing fails")
  void authenticate_withMalformedToken_throwsBadCredentialsException() {
    String token = "invalid.token";
    BearerTokenAuthenticationToken authRequest = new BearerTokenAuthenticationToken(token);

    when(jwtUtil.extractUsername(token)).thenThrow(new JwtException("Invalid token"));

    BadCredentialsException exception =
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(authRequest));

    assertEquals("Invalid token format", exception.getMessage());
    assertNotNull(exception.getCause());

    verify(jwtUtil).extractUsername(token);
    verify(jwtUtil, never()).validateToken(any(), any());
    verify(userDetailsService, never()).loadUserByUsername(any());
  }

  @Test
  @DisplayName("Should throw BadCredentialsException when token validation fails")
  void authenticate_withInvalidToken_throwsBadCredentialsException() {
    String token = "expired.jwt.token";
    BearerTokenAuthenticationToken authRequest = new BearerTokenAuthenticationToken(token);

    when(jwtUtil.extractUsername(token)).thenReturn("testuser");
    when(jwtUtil.validateToken(token, "testuser")).thenReturn(false);

    BadCredentialsException exception =
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(authRequest));

    assertEquals("Invalid or expired token", exception.getMessage());

    verify(jwtUtil).extractUsername(token);
    verify(jwtUtil).validateToken(token, "testuser");
    verify(userDetailsService, never()).loadUserByUsername(any());
  }

  @Test
  @DisplayName("Should propagate exception when user not found")
  void authenticate_withNonExistentUser_throwsUsernameNotFoundException() {
    String token = "valid.jwt.token";
    BearerTokenAuthenticationToken authRequest = new BearerTokenAuthenticationToken(token);

    when(jwtUtil.extractUsername(token)).thenReturn("unknownuser");
    when(jwtUtil.validateToken(token, "unknownuser")).thenReturn(true);
    when(userDetailsService.loadUserByUsername("unknownuser"))
        .thenThrow(new UsernameNotFoundException("User not found"));

    assertThrows(UsernameNotFoundException.class, () -> provider.authenticate(authRequest));

    verify(jwtUtil).extractUsername(token);
    verify(jwtUtil).validateToken(token, "unknownuser");
    verify(userDetailsService).loadUserByUsername("unknownuser");
  }

  @Test
  @DisplayName("Should throw AuthenticationException for unsupported authentication type")
  void authenticate_withUnsupportedAuthenticationType_throwsException() {
    UsernamePasswordAuthenticationToken authRequest =
        new UsernamePasswordAuthenticationToken("user", "pass");

    AuthenticationException exception =
        assertThrows(AuthenticationException.class, () -> provider.authenticate(authRequest));

    assertEquals("Unsupported authentication type", exception.getMessage());
    verify(jwtUtil, never()).extractUsername(any());
    verify(jwtUtil, never()).validateToken(any(), any());
    verify(userDetailsService, never()).loadUserByUsername(any());
  }
}
