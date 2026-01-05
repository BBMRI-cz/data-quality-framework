package eu.bbmri_eric.quality.agent.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;

import lombok.Getter;
import org.springframework.hateoas.server.core.Relation;

/** DTO for quality check result data. */
@Getter
@Schema(name = "Quality Check Result", description = "Result of a quality check execution")
@Relation(itemRelation = "result", collectionRelation = "results")
public final class QualityCheckResultDTO {
  @Schema(
      description = "Hash identifying the quality check",
      example = "abc123def456",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Quality check hash cannot be blank")
  @Size(min = 1, max = 255, message = "Hash must be between 1 and 255 characters")
  @Pattern(
      regexp = "^[a-zA-Z0-9_-]+$",
      message = "Hash must contain only alphanumeric characters, underscores, and hyphens")
  private final String hash;

  @Schema(
      description = "Optional name for the quality check",
      example = "Completeness Check",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @Size(max = 255, message = "Name must be at most 255 characters")
  private String name = "";

  @Schema(
      description = "Numeric result of the quality check",
      example = "0.95",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Result cannot be null")
  private final Double result;

  /** Constructor with hash and result only. */
  public QualityCheckResultDTO(String hash, Double result) {
    this.hash = hash;
    this.name = "";
    this.result = result;
  }

  /** Constructor with hash, name, and result. */
  public QualityCheckResultDTO(String hash, String name, Double result) {
    this.hash = hash;
    this.name = name != null ? name : "";
    this.result = result;
  }

    @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (QualityCheckResultDTO) obj;
    return Objects.equals(this.hash, that.hash)
        && Objects.equals(this.name, that.name)
        && Objects.equals(this.result, that.result);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hash, name, result);
  }

  @Override
  public String toString() {
    return "QualityCheckResultDTO["
        + "hash="
        + hash
        + ", "
        + "name="
        + name
        + ", "
        + "result="
        + result
        + ']';
  }
}
