package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.hateoas.server.core.Relation;

/** DTO for manifest metadata, including its published versions. */
@Schema(name = "Manifest", description = "Metadata of a quality check manifest")
@Relation(itemRelation = "manifest", collectionRelation = "manifests")
public class ManifestDTO {

  @Schema(
      description = "Numeric id of the manifest",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  @Schema(
      description = "Name of the manifest",
      example = "Quality Checks 2026-08",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(description = "Published versions of this manifest")
  private List<ManifestVersionDTO> versions;

  /** Default constructor for serialization frameworks. */
  public ManifestDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param name the name of the manifest
   * @param versions the published versions of this manifest
   */
  public ManifestDTO(String name, List<ManifestVersionDTO> versions) {
    this.name = name;
    this.versions = versions;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<ManifestVersionDTO> getVersions() {
    return versions;
  }

  public void setVersions(List<ManifestVersionDTO> versions) {
    this.versions = versions;
  }
}
