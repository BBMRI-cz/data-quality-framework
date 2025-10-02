package eu.bbmri_eric.quality.server.auth;

import eu.bbmri_eric.quality.server.user.UserDTO;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object for login responses containing JWT token. Using Java Record for immutable
 * data transfer.
 */
@Schema(description = "Login response containing JWT token and user information")
public record LoginResponse(
    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,
    @Schema(description = "Authenticated user information") UserDTO user) {}
