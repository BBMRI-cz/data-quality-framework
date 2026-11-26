package eu.bbmri_eric.quality.agent.server.impl.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.server.RegistrationFailedException;
import eu.bbmri_eric.quality.agent.server.RegistrationFailureReason;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.net.ssl.SSLException;
import org.apache.hc.core5.http.ConnectionRequestTimeoutException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

/**
 * Classifies low-level REST client failures of a registration request into {@link
 * RegistrationFailedException}s carrying a {@link RegistrationFailureReason}. HTTP error responses
 * are classified by status and their RFC-7807 ProblemDetail {@code detail} is extracted as the
 * failure message; all other client failures (I/O errors, including timeouts while reading the
 * response body) are classified by walking the cause chain for timeouts, TLS problems and
 * unreachable hosts.
 */
final class RegistrationFailureClassifier {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final int DETAIL_MAX_LENGTH = 500;
  private static final int MAX_CAUSE_DEPTH = 5;

  /**
   * Classifies a failure that occurred while sending a registration request.
   *
   * @param failure the original client exception
   * @return the classified registration failure
   */
  RegistrationFailedException classify(RestClientException failure) {
    return switch (failure) {
      case HttpStatusCodeException response -> fromHttpResponse(response);
      default -> fromCauseChain(failure);
    };
  }

  private static RegistrationFailedException fromHttpResponse(HttpStatusCodeException response) {
    String message =
        problemDetail(response.getResponseBodyAsString())
            .or(() -> Optional.ofNullable(response.getStatusText()).filter(text -> !text.isBlank()))
            .orElse("HTTP " + response.getStatusCode().value());
    return RegistrationFailedException.forHttpStatus(response.getStatusCode(), message, response);
  }

  private static RegistrationFailedException fromCauseChain(RestClientException failure) {
    List<Throwable> causeChain =
        Stream.iterate(failure, Objects::nonNull, Throwable::getCause)
            .limit(MAX_CAUSE_DEPTH)
            .toList();
    RegistrationFailureReason reason =
        causeChain.stream()
            .map(RegistrationFailureClassifier::reasonFor)
            .flatMap(Optional::stream)
            .findFirst()
            .orElse(RegistrationFailureReason.UNKNOWN);
    Throwable deepestCause = causeChain.getLast();
    String message =
        deepestCause.getMessage() != null ? deepestCause.getMessage() : failure.getMessage();
    return RegistrationFailedException.forReason(reason, message, failure);
  }

  private static Optional<RegistrationFailureReason> reasonFor(Throwable cause) {
    return switch (cause) {
      case SocketTimeoutException _, ConnectionRequestTimeoutException _, HttpTimeoutException _ ->
          Optional.of(RegistrationFailureReason.TIMEOUT);
      case SSLException _ -> Optional.of(RegistrationFailureReason.TLS);
      case UnknownHostException _, ConnectException _, NoRouteToHostException _ ->
          Optional.of(RegistrationFailureReason.UNREACHABLE);
      default -> Optional.empty();
    };
  }

  private static Optional<String> problemDetail(String body) {
    if (body == null || !body.trim().startsWith("{")) {
      return Optional.empty();
    }
    try {
      JsonNode node = OBJECT_MAPPER.readTree(body);
      JsonNode detail = node.get("detail");
      if (detail == null || !detail.isTextual()) {
        return Optional.empty();
      }
      String text = detail.asText();
      return Optional.of(
          text.length() > DETAIL_MAX_LENGTH ? text.substring(0, DETAIL_MAX_LENGTH) : text);
    } catch (JsonProcessingException e) {
      return Optional.empty();
    }
  }
}
