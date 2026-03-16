package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO for adding a keyword to a quality check. */
@Schema(name = "Keyword", description = "Data for adding a keyword to a quality check")
public class KeywordDTO {

  @NotBlank(message = "Keyword cannot be blank")
  @Size(min = 1, max = 250, message = "Keyword must be between 1 and 250 characters")
  @Schema(
      description = "Keyword to add to the quality check",
      example = "patient data",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String keyword;

  public KeywordDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param keyword the keyword
   */
  public KeywordDTO(String keyword) {
    this.keyword = keyword;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }
}
