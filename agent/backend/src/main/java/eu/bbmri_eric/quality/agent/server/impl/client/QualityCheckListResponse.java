package eu.bbmri_eric.quality.agent.server.impl.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Wire representation of the central server's HAL collection of quality checks pinned by a manifest
 * version.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class QualityCheckListResponse {

  @JsonProperty("_embedded")
  private EmbeddedQualityChecks embedded;

  List<RemoteQualityCheck> getQualityChecks() {
    return embedded == null || embedded.qualityChecks == null ? List.of() : embedded.qualityChecks;
  }

  void setEmbedded(EmbeddedQualityChecks embedded) {
    this.embedded = embedded;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class EmbeddedQualityChecks {

    @JsonProperty("qualityChecks")
    private List<RemoteQualityCheck> qualityChecks;

    void setQualityChecks(List<RemoteQualityCheck> qualityChecks) {
      this.qualityChecks = qualityChecks;
    }
  }

  /** Wire representation of a quality check together with its pinned version. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class RemoteQualityCheck {

    private Long id;
    private String name;
    private String description;
    private double warningThreshold;
    private double errorThreshold;
    private RemoteCategory category;
    private List<RemoteQualityCheckVersion> versions = List.of();

    Long getId() {
      return id;
    }

    void setId(Long id) {
      this.id = id;
    }

    String getName() {
      return name;
    }

    void setName(String name) {
      this.name = name;
    }

    String getDescription() {
      return description;
    }

    void setDescription(String description) {
      this.description = description;
    }

    double getWarningThreshold() {
      return warningThreshold;
    }

    void setWarningThreshold(double warningThreshold) {
      this.warningThreshold = warningThreshold;
    }

    double getErrorThreshold() {
      return errorThreshold;
    }

    void setErrorThreshold(double errorThreshold) {
      this.errorThreshold = errorThreshold;
    }

    RemoteCategory getCategory() {
      return category;
    }

    void setCategory(RemoteCategory category) {
      this.category = category;
    }

    List<RemoteQualityCheckVersion> getVersions() {
      return versions;
    }

    void setVersions(List<RemoteQualityCheckVersion> versions) {
      this.versions = versions;
    }
  }

  /** Wire representation of a quality check category on the central server. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class RemoteCategory {

    private Long id;
    private String name;
    private String colorHex;

    Long getId() {
      return id;
    }

    void setId(Long id) {
      this.id = id;
    }

    String getName() {
      return name;
    }

    void setName(String name) {
      this.name = name;
    }

    String getColorHex() {
      return colorHex;
    }

    void setColorHex(String colorHex) {
      this.colorHex = colorHex;
    }
  }

  /** Wire representation of a single pinned quality check version. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class RemoteQualityCheckVersion {

    private Long id;
    private int version;
    private String query;
    private String hash;
    private String type;

    Long getId() {
      return id;
    }

    void setId(Long id) {
      this.id = id;
    }

    int getVersion() {
      return version;
    }

    void setVersion(int version) {
      this.version = version;
    }

    String getQuery() {
      return query;
    }

    void setQuery(String query) {
      this.query = query;
    }

    String getHash() {
      return hash;
    }

    void setHash(String hash) {
      this.hash = hash;
    }

    String getType() {
      return type;
    }

    void setType(String type) {
      this.type = type;
    }
  }
}
