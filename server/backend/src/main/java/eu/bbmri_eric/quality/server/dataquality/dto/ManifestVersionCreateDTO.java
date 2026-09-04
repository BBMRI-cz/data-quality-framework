package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** DTO for publishing a new version of a manifest. */
@Schema(name = "Manifest Version Create", description = "Data for publishing a manifest version")
public class ManifestVersionCreateDTO {

  @NotEmpty(message = "At least one check hash is required")
  @Schema(
      description =
          "SHA-256 hashes of the quality check versions to include in the manifest version",
      example = "[\"5f3c9a...\"]",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private List<String> hashes;

  @Min(value = 1, message = "Version must be at least 1")
  @Schema(
      description = "Version number; if omitted, the next available version is assigned",
      example = "1")
  private Integer version;

  /** Default constructor for serialization frameworks. */
  public ManifestVersionCreateDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param hashes the SHA-256 hashes of the quality check versions to include
   * @param version the version number, or null to auto-assign
   */
  public ManifestVersionCreateDTO(List<String> hashes, Integer version) {
    this.hashes = hashes;
    this.version = version;
  }

  public List<String> getHashes() {
    return hashes;
  }

  public void setHashes(List<String> hashes) {
    this.hashes = hashes;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
