package eu.bbmri_eric.quality.agent.auth;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.quality.agent.common.BasicAuthSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@SpringBootTest
class BasicAuthSecurityTest {

  @Autowired private SecurityFilterChain securityFilterChain;

  @Autowired private BasicAuthSecurity basicAuthSecurity;

  @Test
  void exposesSecurityFilterChainBean() {
    assertThat(securityFilterChain).isNotNull();
  }

  @Test
  void configuresCors() {
    UrlBasedCorsConfigurationSource source =
        (UrlBasedCorsConfigurationSource) basicAuthSecurity.corsConfigurationSource();
    CorsConfiguration config = source.getCorsConfigurations().get("/**");

    assertThat(config).isNotNull();
    assertThat(config.getAllowedOriginPatterns()).containsExactly("*");
    assertThat(config.getAllowedMethods())
        .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    assertThat(config.getAllowedHeaders())
        .containsExactlyInAnyOrder(
            "Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With");
    assertThat(config.getAllowCredentials()).isTrue();
    assertThat(config.getMaxAge()).isEqualTo(3600L);
  }

  @Test
  void returnsArgon2PasswordEncoderBean() {
    Argon2PasswordEncoder encoder =
        (Argon2PasswordEncoder) basicAuthSecurity.argon2PasswordEncoder();
    assertThat(encoder).isNotNull();
    assertThat(encoder).isInstanceOf(Argon2PasswordEncoder.class);
  }
}
