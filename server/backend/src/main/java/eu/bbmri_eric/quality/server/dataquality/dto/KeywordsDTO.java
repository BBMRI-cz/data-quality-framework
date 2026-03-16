package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** DTO for setting keywords on a quality check. */
@Schema(name = "Keywords", description = "Data for setting keywords on a quality check")
public class KeywordsDTO {

  @NotNull(message = "Keywords cannot be null")
  @Schema(
      description =
          "Set of keywords to assign to the quality check (replaces all existing keywords)",
      example = "[\"patient data\", \"gender\", \"diagnosis\"]",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Set<@Size(max = 250, message = "Each keyword must be at most 250 characters") String>
      keywords;

  public KeywordsDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param keywords the set of keywords
   */
  public KeywordsDTO(Set<String> keywords) {
    this.keywords = keywords;
  }

  public Set<String> getKeywords() {
    return keywords;
  }

  public void setKeywords(Set<String> keywords) {
    this.keywords = keywords;
  }
}
