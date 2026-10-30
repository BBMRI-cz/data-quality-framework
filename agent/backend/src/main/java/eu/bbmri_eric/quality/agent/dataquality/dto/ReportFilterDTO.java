package eu.bbmri_eric.quality.agent.dataquality.dto;

import eu.bbmri_eric.quality.agent.common.dto.FilterDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Filter DTO for report queries; light on purpose — defaults sort to {@code generatedAt}/DESC at
 * construction and rejects unknown sort properties at assignment time, so the service never
 * special-cases them.
 */
@Setter
@Getter
@Schema(description = "Filter parameters for listing reports")
public class ReportFilterDTO extends FilterDTO {

  private static final Set<String> ALLOWED_SORT_PROPERTIES =
      Set.of(
          "id",
          "generatedAt",
          "status",
          "epsilonBudget",
          "numberOfEntities",
          "numberOfSecondaryEntities");

  private static final String DEFAULT_SORT_PROPERTY = "generatedAt";

  /** Builds a filter with the report-domain defaults: {@code generatedAt} DESC. */
  public ReportFilterDTO() {
    super();
    setSort(DEFAULT_SORT_PROPERTY);
    setOrder(SortOrder.DESC);
  }

  public ReportFilterDTO(int page, int size, String sort, SortOrder order) {
    super(
        page,
        size,
        sort == null ? DEFAULT_SORT_PROPERTY : sort,
        order != null ? order : SortOrder.DESC);
  }

  @Override
  public void setSort(String sort) {
    if (sort != null && !ALLOWED_SORT_PROPERTIES.contains(sort)) {
      throw new IllegalArgumentException("Cannot sort reports by: " + sort);
    }
    super.setSort(sort);
  }
}
