package eu.bbmri_eric.quality.server.user;

import jakarta.validation.constraints.NotBlank;

/** Data Transfer Object for login requests. Using Java Record for immutable data transfer. */
public record LoginRequest(
    @NotBlank(message = "Username is required") String username,
    @NotBlank(message = "Password is required") String password) {}
