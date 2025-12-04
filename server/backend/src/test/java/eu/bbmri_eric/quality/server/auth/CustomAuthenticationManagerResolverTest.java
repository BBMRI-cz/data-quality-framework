package eu.bbmri_eric.quality.server.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;

/** Unit tests for CustomAuthenticationManagerResolver. */
@ExtendWith(MockitoExtension.class)
class CustomAuthenticationManagerResolverTest {

  @Mock private JwtUtil jwtUtil;

  @Mock private JwtAuthenticationConverter jwtAuthenticationConverter;

  @Mock private UserDetailsService userDetailsService;

  @Mock private HttpServletRequest request;

  private CustomAuthenticationManagerResolver resolver;
  private static final String INTERNAL_ISSUER = "quality-server";
  private static final String BEARER_TOKEN = "Bearer valid.jwt.token";
  private static final String JWT_TOKEN = "valid.jwt.token";

  @Test
  @DisplayName("Should resolve internal authentication manager for internal issuer")
  void resolve_withInternalIssuer_returnsInternalAuthManager() {
    resolver =
        new CustomAuthenticationManagerResolver(
            jwtUtil, jwtAuthenticationConverter, userDetailsService, null);

    when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
    when(jwtUtil.extractIssuer(JWT_TOKEN)).thenReturn(INTERNAL_ISSUER);

    AuthenticationManager authManager = resolver.resolve(request);

    assertNotNull(authManager);
    verify(request).getHeader("Authorization");
    verify(jwtUtil).extractIssuer(JWT_TOKEN);
  }

  @Test
  @DisplayName("Should throw IllegalStateException when Authorization header is missing")
  void resolve_withMissingAuthHeader_throwsIllegalStateException() {
    resolver =
        new CustomAuthenticationManagerResolver(
            jwtUtil, jwtAuthenticationConverter, userDetailsService, null);

    when(request.getHeader("Authorization")).thenReturn(null);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> resolver.resolve(request));

    assertEquals(
        "Missing or invalid Authorization header. Expected format: 'Bearer <token>'",
        exception.getMessage());
    verify(request).getHeader("Authorization");
    verify(jwtUtil, never()).extractIssuer(any());
  }

  @Test
  @DisplayName(
      "Should throw IllegalStateException when Authorization header doesn't start with Bearer")
  void resolve_withInvalidAuthHeader_throwsIllegalStateException() {
    resolver =
        new CustomAuthenticationManagerResolver(
            jwtUtil, jwtAuthenticationConverter, userDetailsService, null);

    when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> resolver.resolve(request));

    assertEquals(
        "Missing or invalid Authorization header. Expected format: 'Bearer <token>'",
        exception.getMessage());
    verify(request).getHeader("Authorization");
    verify(jwtUtil, never()).extractIssuer(any());
  }

  @Test
  @DisplayName("Should throw IllegalStateException when Bearer token is empty")
  void resolve_withEmptyToken_throwsIllegalStateException() {
    resolver =
        new CustomAuthenticationManagerResolver(
            jwtUtil, jwtAuthenticationConverter, userDetailsService, null);

    when(request.getHeader("Authorization")).thenReturn("Bearer ");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> resolver.resolve(request));

    assertEquals("Bearer token cannot be empty", exception.getMessage());
    verify(request).getHeader("Authorization");
    verify(jwtUtil, never()).extractIssuer(any());
  }

  @Test
  @DisplayName("Should throw IllegalStateException when Bearer token is blank")
  void resolve_withBlankToken_throwsIllegalStateException() {
    resolver =
        new CustomAuthenticationManagerResolver(
            jwtUtil, jwtAuthenticationConverter, userDetailsService, null);

    when(request.getHeader("Authorization")).thenReturn("Bearer    ");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> resolver.resolve(request));

    assertEquals("Bearer token cannot be empty", exception.getMessage());
    verify(request).getHeader("Authorization");
    verify(jwtUtil, never()).extractIssuer(any());
  }

  @Test
  @DisplayName("Should throw IllegalStateException when extracting issuer fails")
  void resolve_whenExtractIssuerFails_throwsIllegalStateException() {
    resolver =
        new CustomAuthenticationManagerResolver(
            jwtUtil, jwtAuthenticationConverter, userDetailsService, null);

    when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
    when(jwtUtil.extractIssuer(JWT_TOKEN)).thenThrow(new RuntimeException("Failed to parse token"));

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> resolver.resolve(request));

    assertTrue(exception.getMessage().startsWith("Failed to resolve AuthenticationManager:"));
    assertNotNull(exception.getCause());
    verify(request).getHeader("Authorization");
    verify(jwtUtil).extractIssuer(JWT_TOKEN);
  }
}
