package eu.bbmri_eric.quality.server.crypto.controller;

import eu.bbmri_eric.quality.server.crypto.KeyProvider;
import eu.bbmri_eric.quality.server.crypto.dto.PublicKeyDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.PublicKey;
import java.util.Base64;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for exposing cryptographic material. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Crypto", description = "API for cryptographic key operations")
class CryptoController {

  private final KeyProvider keyProvider;

  public CryptoController(KeyProvider keyProvider) {
    this.keyProvider = keyProvider;
  }

  @GetMapping("/public-key")
  @Operation(
      summary = "Get the public key",
      description = "Retrieves the server's public key used for verifying signatures")
  @SecurityRequirements
  public ResponseEntity<PublicKeyDTO> getPublicKey() {
    PublicKey publicKey = keyProvider.getPublicKey();
    PublicKeyDTO dto =
        new PublicKeyDTO(
            keyProvider.getKeyId(), Base64.getEncoder().encodeToString(publicKey.getEncoded()));
    return ResponseEntity.ok(dto);
  }
}
