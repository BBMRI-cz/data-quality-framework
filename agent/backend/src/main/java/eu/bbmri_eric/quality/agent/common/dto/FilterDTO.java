package eu.bbmri_eric.quality.agent.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Common data transfer object for filtering, pagination, and sorting.
 *
 * <p>This DTO provides standard fields for querying collections with pagination and sorting
 * capabilities. It can be extended for domain-specific filtering needs.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Schema(description = "Common filter DTO for pagination and sorting")
public class FilterDTO {

  @Schema(description = "Page number (zero-based)", example = "0", defaultValue = "0")
  @Min(value = 0, message = "Page number must be non-negative")
  private int page = 0;

  @Schema(description = "Page size (number of items per page)", example = "20", defaultValue = "20")
  @Min(value = 1, message = "Page size must be at least 1")
  private int size = 20;

  @Schema(description = "Field name to sort by", example = "id")
  private String sort;

  @Schema(
      description = "Sort order (ASC or DESC)",
      example = "ASC",
      allowableValues = {"ASC", "DESC"},
      defaultValue = "DESC")
  private SortOrder order = SortOrder.DESC;

  /**
   * Sets the sort property; the base filter accepts everything. Domain filters override to fail
   * fast on properties they do not support, so an invalid value never lands in the DTO at all.
   */
  @Schema(hidden = true)
  public void setSort(String sort) {
    this.sort = sort;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FilterDTO filterDTO = (FilterDTO) o;
    return page == filterDTO.page
        && size == filterDTO.size
        && Objects.equals(sort, filterDTO.sort)
        && order == filterDTO.order;
  }

  @Override
  public int hashCode() {
    return Objects.hash(page, size, sort, order);
  }

  /** Enum representing sort order. */
  public enum SortOrder {
    /** Ascending order. */
    ASC,
    /** Descending order. */
    DESC
  }
}
