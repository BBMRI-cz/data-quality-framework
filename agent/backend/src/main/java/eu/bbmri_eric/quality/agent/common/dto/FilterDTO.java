package eu.bbmri_eric.quality.agent.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.util.Objects;

/**
 * Common data transfer object for filtering, pagination, and sorting.
 *
 * <p>This DTO provides standard fields for querying collections with pagination and sorting
 * capabilities. It can be extended for domain-specific filtering needs.
 */
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
      defaultValue = "ASC")
  private SortOrder order = SortOrder.ASC;

  /** Default constructor. */
  public FilterDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param page the page number (zero-based)
   * @param size the page size
   * @param sort the field name to sort by
   * @param order the sort order
   */
  public FilterDTO(int page, int size, String sort, SortOrder order) {
    this.page = page;
    this.size = size;
    this.sort = sort;
    this.order = order;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public String getSort() {
    return sort;
  }

  public void setSort(String sort) {
    this.sort = sort;
  }

  public SortOrder getOrder() {
    return order;
  }

  public void setOrder(SortOrder order) {
    this.order = order;
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

  @Override
  public String toString() {
    return "FilterDTO{"
        + "page="
        + page
        + ", size="
        + size
        + ", sort='"
        + sort
        + '\''
        + ", order="
        + order
        + '}';
  }

  /** Enum representing sort order. */
  public enum SortOrder {
    /** Ascending order. */
    ASC,
    /** Descending order. */
    DESC
  }
}
