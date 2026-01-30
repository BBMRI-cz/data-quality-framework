package eu.bbmri_eric.quality.agent.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/**
 * Generic page response containing paginated data and metadata.
 *
 * @param <T> the type of elements in the page
 */
@Setter
@Getter
@Schema(description = "Paginated response with metadata")
public class PageResponse<T> {

  @Schema(
      description = "The list of items in this page",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private List<T> content;

  @Schema(description = "Current page number (zero-based)", example = "0")
  private int page;

  @Schema(description = "Number of items per page", example = "20")
  private int size;

  @Schema(description = "Total number of elements across all pages", example = "100")
  private long totalElements;

  @Schema(description = "Total number of pages", example = "5")
  private int totalPages;

  @Schema(description = "Whether this is the first page")
  private boolean first;

  @Schema(description = "Whether this is the last page")
  private boolean last;

  @Schema(description = "Whether there are more pages after this one")
  private boolean hasNext;

  @Schema(description = "Whether there are pages before this one")
  private boolean hasPrevious;

  /** Default constructor for serialization frameworks. */
  public PageResponse() {}

  /**
   * Creates a page response with all metadata.
   *
   * @param content the list of items in this page
   * @param page the current page number (zero-based)
   * @param size the number of items per page
   * @param totalElements the total number of elements across all pages
   */
  public PageResponse(List<T> content, int page, int size, long totalElements) {
    this.content = content;
    this.page = page;
    this.size = size;
    this.totalElements = totalElements;
    this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
    this.first = page == 0;
    this.last = page >= totalPages - 1;
    this.hasNext = page < totalPages - 1;
    this.hasPrevious = page > 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PageResponse<?> that = (PageResponse<?>) o;
    return page == that.page
        && size == that.size
        && totalElements == that.totalElements
        && totalPages == that.totalPages
        && first == that.first
        && last == that.last
        && hasNext == that.hasNext
        && hasPrevious == that.hasPrevious
        && Objects.equals(content, that.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        content, page, size, totalElements, totalPages, first, last, hasNext, hasPrevious);
  }

  @Override
  public String toString() {
    return "PageResponse{"
        + "content="
        + content
        + ", page="
        + page
        + ", size="
        + size
        + ", totalElements="
        + totalElements
        + ", totalPages="
        + totalPages
        + ", first="
        + first
        + ", last="
        + last
        + ", hasNext="
        + hasNext
        + ", hasPrevious="
        + hasPrevious
        + '}';
  }
}
