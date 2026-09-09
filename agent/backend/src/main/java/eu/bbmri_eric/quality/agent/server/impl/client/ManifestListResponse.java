package eu.bbmri_eric.quality.agent.server.impl.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.bbmri_eric.quality.agent.server.dto.ManifestDto;
import java.util.List;

/** Wire representation of the central server's HAL collection of manifests. */
@JsonIgnoreProperties(ignoreUnknown = true)
class ManifestListResponse {

  @JsonProperty("_embedded")
  private EmbeddedManifests embedded;

  List<ManifestDto> getManifests() {
    return embedded == null || embedded.manifests == null ? List.of() : embedded.manifests;
  }

  void setEmbedded(EmbeddedManifests embedded) {
    this.embedded = embedded;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class EmbeddedManifests {

    @JsonProperty("manifests")
    private List<ManifestDto> manifests;

    void setManifests(List<ManifestDto> manifests) {
      this.manifests = manifests;
    }
  }
}
