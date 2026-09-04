package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** DTO for creating a new manifest. */
@Schema(name = "Manifest Create", description = "Data for creating a quality check manifest")
public class ManifestCreateDTO {

  @NotBlank(message = "Name cannot be blank")
  @Schema(
      description = "Name of the manifest",
      example = "Quality Checks 2026-08",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  /** Default constructor for serialization frameworks. */
  public ManifestCreateDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param name the name of the manifest
   */
  public ManifestCreateDTO(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
