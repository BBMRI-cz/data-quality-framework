package eu.bbmri_eric.quality.agent.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Result of downloading and installing a manifest version on the agent. */
@Schema(
    name = "Manifest Download",
    description = "Result of downloading and installing a manifest version")
public class ManifestDownloadDto {

  @Schema(
      description = "Local ID of the persisted manifest",
      example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
  private String id;

  @Schema(description = "ID of the manifest on the central server", example = "1")
  private Long remoteId;

  @Schema(description = "Name of the manifest", example = "Quality Checks 2026-08")
  private String name;

  @Schema(description = "Version number installed locally", example = "2")
  private int installedVersion;

  @Schema(description = "Number of quality checks installed from this version", example = "12")
  private int installedChecks;

  /** Default constructor for serialization frameworks. */
  public ManifestDownloadDto() {}

  public ManifestDownloadDto(
      String id, Long remoteId, String name, int installedVersion, int installedChecks) {
    this.id = id;
    this.remoteId = remoteId;
    this.name = name;
    this.installedVersion = installedVersion;
    this.installedChecks = installedChecks;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public int getInstalledVersion() {
    return installedVersion;
  }

  public void setInstalledVersion(int installedVersion) {
    this.installedVersion = installedVersion;
  }

  public int getInstalledChecks() {
    return installedChecks;
  }

  public void setInstalledChecks(int installedChecks) {
    this.installedChecks = installedChecks;
  }
}
