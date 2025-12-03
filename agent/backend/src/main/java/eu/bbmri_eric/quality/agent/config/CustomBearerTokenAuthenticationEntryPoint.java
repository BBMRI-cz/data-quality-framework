package eu.bbmri_eric.quality.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Custom implementation of {@link org.springframework.security.web.AuthenticationEntryPoint} that
 * returns a ProblemDetail response for HTTP error 401 (Unauthorized).
 */
@Component
class CustomBearerTokenAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private static final String PROBLEM_JSON_CONTENT_TYPE = "application/problem+json";
  private static final URI AUTH_ERROR_URI =
      URI.create("https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/401");
  private static final HttpStatus HTTP_STATUS = HttpStatus.UNAUTHORIZED;

  private final ObjectMapper objectMapper;

  CustomBearerTokenAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HTTP_STATUS, authException.getMessage());
    problemDetail.setTitle(HTTP_STATUS.getReasonPhrase());
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setType(AUTH_ERROR_URI);
    problemDetail.setProperties(Map.of());

    response.setStatus(HTTP_STATUS.value());
    response.setContentType(PROBLEM_JSON_CONTENT_TYPE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    response.getOutputStream().write(objectMapper.writeValueAsBytes(problemDetail));
  }
}
