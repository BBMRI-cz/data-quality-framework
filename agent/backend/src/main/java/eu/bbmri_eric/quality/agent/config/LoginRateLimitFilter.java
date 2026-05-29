package eu.bbmri_eric.quality.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.user.LoginAttemptService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Prevents brute-force login attempts on the login endpoint. Blocks requests from IPs that have
 * exceeded the threshold of failed attempts and records outcomes based on the HTTP response status
 * after the request has been processed.
 */
@Component
class LoginRateLimitFilter extends OncePerRequestFilter {

  private final LoginAttemptService loginAttemptService;
  private final ObjectMapper objectMapper;

  LoginRateLimitFilter(LoginAttemptService loginAttemptService, ObjectMapper objectMapper) {
    this.loginAttemptService = loginAttemptService;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    if (!isLoginRequest(req)) {
      chain.doFilter(req, res);
      return;
    }

    String ip = req.getRemoteAddr() != null ? req.getRemoteAddr() : "unknown";
    if (loginAttemptService.isBlocked(ip)) {
      ProblemDetail problemDetail = buildResponse(req);
      res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      res.getWriter().write(objectMapper.writeValueAsString(problemDetail));
      return;
    }

    chain.doFilter(req, res);

    int status = res.getStatus();
    if (status == HttpStatus.UNAUTHORIZED.value()) {
      loginAttemptService.recordFailure(ip);
    } else if (status >= HttpStatus.OK.value() && status < HttpStatus.MULTIPLE_CHOICES.value()) {
      loginAttemptService.recordSuccess(ip);
    }
  }

  private static @NonNull ProblemDetail buildResponse(HttpServletRequest req) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Try again later.");
    problemDetail.setTitle("Too Many Requests");
    problemDetail.setInstance(URI.create(req.getRequestURI()));
    return problemDetail;
  }

  private boolean isLoginRequest(HttpServletRequest req) {
    return "/api/auth/login".equals(req.getRequestURI())
        && "POST".equalsIgnoreCase(req.getMethod());
  }
}
