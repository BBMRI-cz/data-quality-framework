package eu.bbmri_eric.quality.agent.config;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

/** Global handler for translating exceptions into HTTP responses */
@ControllerAdvice
class GlobalRestExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(GlobalRestExceptionHandler.class);

  @ExceptionHandler(org.hibernate.StaleStateException.class)
  @ApiResponse(
      responseCode = "404",
      description = "Entity Not Found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleStaleState(org.hibernate.StaleStateException ex) {
    logger.debug("Stale state exception: {}", ex.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Entity not found");
    problemDetail.setTitle("Entity Not Found");
    return problemDetail;
  }

  @ExceptionHandler(EntityNotFoundException.class)
  @ApiResponse(
      responseCode = "404",
      description = "Entity Not Found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleStaleState(EntityNotFoundException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Entity not found");
    problemDetail.setTitle("Entity Not Found");
    return problemDetail;
  }

  @ExceptionHandler(ConversionFailedException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleFailedConversion(ConversionFailedException ex) {
    logger.debug("Conversion failed: {}", ex.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setTitle("Bad Request");
    return problemDetail;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    logger.debug("Illegal argument: {}", ex.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setTitle("Bad Request");
    return problemDetail;
  }

  @ExceptionHandler(UsernameNotFoundException.class)
  @ApiResponse(
      responseCode = "404",
      description = "User Not Found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleUserNotFound(UsernameNotFoundException ex) {
    logger.debug("User not found: {}", ex.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problemDetail.setTitle("User Not Found");
    return problemDetail;
  }

  @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
  @ApiResponse(
      responseCode = "401",
      description = "Authentication Required",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleAuthenticationRequired(AuthenticationCredentialsNotFoundException ex) {
    logger.debug("Authentication required: {}", ex.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    problemDetail.setTitle("Authentication Required");
    return problemDetail;
  }

  @ExceptionHandler(IllegalStateException.class)
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleIllegalState(IllegalStateException ex) {
    logger.debug("Illegal state: {}", ex.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    problemDetail.setTitle("Forbidden");
    return problemDetail;
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ApiResponse(
      responseCode = "403",
      description = "Access Denied",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
    logger.debug("Access denied: {}", ex.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    problemDetail.setTitle("Access Denied");
    return problemDetail;
  }

  @ExceptionHandler(BadCredentialsException.class)
  @ApiResponse(
      responseCode = "401",
      description = "Bad Credentials",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
    logger.debug("Bad credentials: {}", ex.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    problemDetail.setTitle("Bad Credentials");
    return problemDetail;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Malformed Request",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
    logger.debug("Malformed request: {}", ex.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request");
    problemDetail.setTitle("Malformed Request");
    return problemDetail;
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  @ApiResponse(
      responseCode = "405",
      description = "Method Not Allowed",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException ex) {
    logger.debug("Method not supported: {}", ex.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage());
    problemDetail.setTitle("Method Not Allowed");
    return problemDetail;
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Missing Parameter",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleMissingServletRequestParameter(
      MissingServletRequestParameterException ex) {
    logger.debug("Missing parameter: {}", ex.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setTitle("Missing Parameter");
    return problemDetail;
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  @ApiResponse(
      responseCode = "404",
      description = "Not Found",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleNoHandlerFoundException(NoHandlerFoundException ex) {
    logger.debug("No handler found: {}", ex.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Endpoint not found");
    problemDetail.setTitle("Not Found");
    return problemDetail;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Constraint Violation",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
    logger.debug("Constraint violation: {}", ex.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation constraint violated");
    problemDetail.setTitle("Constraint Violation");

    Map<String, String> violations = new HashMap<>();
    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
      String propertyPath = violation.getPropertyPath().toString();
      violations.put(propertyPath, violation.getMessage());
    }
    problemDetail.setProperty("violations", violations);

    return problemDetail;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  @ApiResponse(
      responseCode = "409",
      description = "Data Integrity Violation",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    logger.debug("Data integrity violation: {}", ex.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Data integrity constraint violated");
    problemDetail.setTitle("Data Integrity Violation");
    return problemDetail;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ApiResponse(
      responseCode = "400",
      description = "Validation Failed",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
    logger.debug("Validation exception: {}", ex.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    problemDetail.setTitle("Validation Failed");

    Map<String, String> validationErrors = new HashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      validationErrors.put(error.getField(), error.getDefaultMessage());
    }
    problemDetail.setProperty("validationErrors", validationErrors);

    return problemDetail;
  }
}
