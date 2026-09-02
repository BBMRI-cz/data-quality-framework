package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** DTO for creating a new manifest. */
@Schema(name = "Manifest Create", description = "Data for creating a quality check manifest")
public class ManifestCreateDTO {

  @NotBlank(message = "Name cannot be blank")
  @Schema(
      description = "Name of the manifest",
      example = "Quality Checks 2026-08",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @NotEmpty(message = "At least one check hash is required")
  @Schema(
      description = "SHA-256 hashes of the quality check versions included in the manifest",
      example = "[\"5f3c9a...\"]",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private List<String> hashes;

  /** Default constructor for serialization frameworks. */
  public ManifestCreateDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param name the name of the manifest
   * @param hashes the SHA-256 hashes of the quality checks to include
   */
  public ManifestCreateDTO(String name, List<String> hashes) {
    this.name = name;
    this.hashes = hashes;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getHashes() {
    return hashes;
  }

  public void setHashes(List<String> hashes) {
    this.hashes = hashes;
  }
}
