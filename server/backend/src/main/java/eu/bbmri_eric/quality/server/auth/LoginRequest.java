package eu.bbmri_eric.quality.server.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Data Transfer Object for login requests. Using Java Record for immutable data transfer. */
@Schema(description = "Login request containing user credentials")
public record LoginRequest(
    @Schema(description = "Username for authentication", example = "admin")
        @NotBlank(message = "Username is required")
        String username,
    @Schema(description = "Password for authentication", example = "password")
        @NotBlank(message = "Password is required")
        String password) {}
