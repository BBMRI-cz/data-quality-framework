package eu.bbmri_eric.quality.agent.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Generic page response containing paginated data and metadata.
 *
 * @param <T> the type of elements in the page
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
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
}
