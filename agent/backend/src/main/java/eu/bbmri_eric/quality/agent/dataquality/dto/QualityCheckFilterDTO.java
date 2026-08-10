package eu.bbmri_eric.quality.agent.dataquality.dto;

import eu.bbmri_eric.quality.agent.common.dto.FilterDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Filter DTO for querying quality checks.
 *
 * <p>Extends the common {@link FilterDTO} with quality-check-specific filters such as category
 * name.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter parameters for listing quality checks")
public class QualityCheckFilterDTO extends FilterDTO {

  @Schema(
      description =
          "Category name to filter by. When omitted, no category filter is applied. When blank, only uncategorized quality checks are returned.",
      example = "Data Completeness")
  private String categoryName;

  /**
   * Constructs a filter with pagination/sorting parameters only.
   *
   * @param page the page number (zero-based)
   * @param size the page size
   * @param sort the sort property
   * @param order the sort order
   */
  public QualityCheckFilterDTO(int page, int size, String sort, SortOrder order) {
    super(page, size, sort, order);
  }
}
