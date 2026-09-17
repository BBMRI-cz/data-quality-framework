package eu.bbmri_eric.quality.agent.server.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Metadata of a quality check manifest published by a central server. */
@Schema(name = "Manifest", description = "Metadata of a quality check manifest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManifestDto {

  @Schema(description = "ID of the manifest on the central server", example = "1")
  @JsonAlias("id")
  private Long remoteId;

  @Schema(description = "Name of the manifest", example = "Quality Checks 2026-08")
  private String name;

  @Schema(description = "Versions of this manifest published by the central server")
  private List<ManifestVersionDto> versions = List.of();

  /** Default constructor for serialization frameworks. */
  public ManifestDto() {}

  public ManifestDto(Long remoteId, String name, List<ManifestVersionDto> versions) {
    this.remoteId = remoteId;
    this.name = name;
    this.versions = versions;
  }

  public Long getRemoteId() {
    return remoteId;
  }

  public void setRemoteId(Long remoteId) {
    this.remoteId = remoteId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<ManifestVersionDto> getVersions() {
    return versions;
  }

  public void setVersions(List<ManifestVersionDto> versions) {
    this.versions = versions;
  }
}
