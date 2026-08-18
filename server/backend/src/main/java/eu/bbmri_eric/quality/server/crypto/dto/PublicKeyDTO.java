package eu.bbmri_eric.quality.server.crypto.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** DTO representing a public key exposed to clients. */
public record PublicKeyDTO(
    @Schema(description = "The key id identifying the public key") String keyId,
    @Schema(description = "The Base64-encoded public key") String publicKey) {}
