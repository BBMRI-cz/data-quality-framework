package eu.bbmri_eric.quality.server.common;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.server.auth.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Application wide security configuration */
@Configuration
@EnableWebSecurity
class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final AuthenticationEntryPoint authenticationEntryPoint;
  private final HttpRequestLoggingFilter httpRequestLoggingFilter;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      AuthenticationEntryPoint authenticationEntryPoint,
      HttpRequestLoggingFilter httpRequestLoggingFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.httpRequestLoggingFilter = httpRequestLoggingFilter;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .anonymous(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(httpRequestLoggingFilter, JwtAuthenticationFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/reports")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/v1/reports/**")
                    .hasAnyRole("HUMAN_USER", "ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/v1/quality-checks/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.PUT, "/api/v1/quality-checks/*")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/v1/categories/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/v1/categories")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/categories/*")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/*")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/v1/agents/*/reports")
                    .hasAnyRole("HUMAN_USER", "ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/v1/agents")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/agents/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/agents/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/agents/*")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/v1/agents/*/reports")
                    .authenticated()
                    .requestMatchers(HttpMethod.PUT, "/api/users/*/password")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/v1/settings")
                    .authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/settings")
                    .authenticated()
                    .requestMatchers(
                        "/api/health",
                        "/api/info",
                        "/api/counts",
                        "/api/swagger-ui.html",
                        "/api/swagger-ui/**",
                        "/api/api-docs/**")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .denyAll() // default deny for all API paths
                    .requestMatchers("/**")
                    .permitAll())
        .exceptionHandling(
            ex ->
                ex.accessDeniedHandler(
                        (request, response, accessDeniedException) ->
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
                    .authenticationEntryPoint(authenticationEntryPoint));
    return http.build();
  }

  @Bean
  PasswordEncoder argon2PasswordEncoder() {
    return new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public AuthenticationEntryPoint authenticationEntryPoint() {
    return new CustomBearerTokenAuthenticationEntryPoint(new ObjectMapper());
  }
}
