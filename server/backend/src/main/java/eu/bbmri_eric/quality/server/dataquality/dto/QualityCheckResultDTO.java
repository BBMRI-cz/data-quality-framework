package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import org.springframework.hateoas.server.core.Relation;

/** DTO for quality check result data. */
@Schema(name = "Quality Check Result", description = "Result of a quality check execution")
@Relation(itemRelation = "result", collectionRelation = "results")
public class QualityCheckResultDTO {

  @NotBlank(message = "Quality check hash cannot be blank")
  @Size(min = 1, max = 255, message = "Hash must be between 1 and 255 characters")
  @Schema(
      description = "Hash identifying the quality check",
      example = "abc123def456",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String hash;

  @Size(max = 255, message = "Name must be at most 255 characters")
  @Schema(
      description = "Optional name for the quality check",
      example = "Completeness Check",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String name = "";

  @NotNull(message = "Result cannot be null")
  @Schema(
      description = "Numeric result of the quality check",
      example = "0.95",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Double result;

  public QualityCheckResultDTO() {}

  public QualityCheckResultDTO(String hash, Double result) {
    this.hash = hash;
    this.result = result;
  }

  public QualityCheckResultDTO(String hash, String name, Double result) {
    this.hash = hash;
    this.name = name != null ? name : "";
    this.result = result;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Double getResult() {
    return result;
  }

  public void setResult(Double result) {
    this.result = result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QualityCheckResultDTO that = (QualityCheckResultDTO) o;
    return Objects.equals(hash, that.hash)
        && Objects.equals(name, that.name)
        && Objects.equals(result, that.result);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hash, name, result);
  }

  @Override
  public String toString() {
    return "QualityCheckResultDTO{"
        + "hash='"
        + hash
        + '\''
        + ", name='"
        + name
        + '\''
        + ", result="
        + result
        + '}';
  }
}
