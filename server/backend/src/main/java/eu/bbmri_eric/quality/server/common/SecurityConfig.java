package eu.bbmri_eric.quality.server.common;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
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

  private final AuthenticationEntryPoint authenticationEntryPoint;
  private final HttpRequestLoggingFilter httpRequestLoggingFilter;
  private final AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver;

  public SecurityConfig(
      AuthenticationEntryPoint authenticationEntryPoint,
      HttpRequestLoggingFilter httpRequestLoggingFilter,
      AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver) {
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.httpRequestLoggingFilter = httpRequestLoggingFilter;
    this.authenticationManagerResolver = authenticationManagerResolver;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .anonymous(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
        .addFilterBefore(httpRequestLoggingFilter, UsernamePasswordAuthenticationFilter.class)
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
                    .requestMatchers(HttpMethod.GET, "/api/v1/groups/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/v1/groups")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/groups/*")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/groups/*/agents")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/groups/*")
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
                    // NOTE: This endpoint is intentionally public to allow the frontend
                    // to bootstrap OIDC configuration (e.g. client ID, redirect URIs).
                    // It MUST NOT return any sensitive data such as client secrets or
                    // credentials. If the response model changes, ensure no secrets
                    // are exposed before keeping this as permitAll().
                    .requestMatchers(HttpMethod.GET, "/api/v1/settings/oidc")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/settings")
                    .authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/settings/oidc")
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
        .oauth2ResourceServer(
            oauth2 -> oauth2.authenticationManagerResolver(authenticationManagerResolver))
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
