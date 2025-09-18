package eu.bbmri_eric.quality.agent.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class HttpRequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    Instant startTime = Instant.now();
    logger.info(
        "Request: {} {} from {}", request.getMethod(), getFullUri(request), getClientIp(request));

    try {
      filterChain.doFilter(request, response);
    } finally {
      Duration duration = Duration.between(startTime, Instant.now());
      logger.info(
          "Response: {} {} -> {} ({}ms)",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          duration.toMillis());
    }
  }

  private String getFullUri(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String queryString = request.getQueryString();
    return queryString != null ? uri + "?" + queryString : uri;
  }

  private String getClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isEmpty()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
