package eu.bbmri_eric.quality.server.dataquality.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

/**
 * The JSON body of a manifest. This structure is what gets serialized and cryptographically signed
 * to preserve its integrity.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ManifestBody {

  private final Long manifestId;
  private final Instant generatedAt;
  private final List<Check> qualityChecks;

  public ManifestBody(Long manifestId, Instant generatedAt, List<Check> checks) {
    this.manifestId = manifestId;
    this.generatedAt = generatedAt;
    this.qualityChecks = checks;
  }

  public Long getManifestId() {
    return manifestId;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public List<Check> getQualityChecks() {
    return qualityChecks;
  }

  /** A single quality check entry within a manifest body. */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public static class Check {

    private final String checkId;
    private final int version;
    private final String sha256;

    public Check(String checkId, int version, String sha256) {
      this.checkId = checkId;
      this.version = version;
      this.sha256 = sha256;
    }

    public String getCheckId() {
      return checkId;
    }

    public int getVersion() {
      return version;
    }

    public String getSha256() {
      return sha256;
    }
  }
}
