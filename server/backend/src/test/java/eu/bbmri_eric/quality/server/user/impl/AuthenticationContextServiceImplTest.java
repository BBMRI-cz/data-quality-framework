package eu.bbmri_eric.quality.server.user.impl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.server.user.domain.User;
import eu.bbmri_eric.quality.server.user.dto.CustomUserDetails;
import eu.bbmri_eric.quality.server.user.dto.UserDTO;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuthenticationContextServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private ModelMapper modelMapper;

  @InjectMocks private AuthenticationContextServiceImpl authenticationContextService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getCurrentUser_withOidcToken_resolvesBySubjectIdNotUsername() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("subject-123")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of(), "subject-123"));
    User user = new User("Display Name", "subject-123");
    UserDTO dto = new UserDTO("Display Name", 7L);
    when(userRepository.findBySubjectId("subject-123")).thenReturn(Optional.of(user));
    when(modelMapper.map(user, UserDTO.class)).thenReturn(dto);

    assertSame(dto, authenticationContextService.getCurrentUser());
    verify(userRepository, never()).findByUsername("subject-123");
  }

  @Test
  void getCurrentUser_withInternalToken_resolvesByUserId() {
    User stored = new User("admin", "encoded", null);
    CustomUserDetails details = new CustomUserDetails(stored);
    details.getUser().setId(3L);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(details, "token", details.getAuthorities()));
    UserDTO dto = new UserDTO("admin", 3L);
    when(userRepository.findById(3L)).thenReturn(Optional.of(stored));
    when(modelMapper.map(stored, UserDTO.class)).thenReturn(dto);

    assertSame(dto, authenticationContextService.getCurrentUser());
    verify(userRepository, never()).findByUsername("admin");
  }
}
