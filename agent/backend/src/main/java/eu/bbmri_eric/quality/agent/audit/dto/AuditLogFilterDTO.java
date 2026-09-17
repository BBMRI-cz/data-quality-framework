package eu.bbmri_eric.quality.agent.audit.dto;

import eu.bbmri_eric.quality.agent.audit.domain.AuditAction;
import eu.bbmri_eric.quality.agent.common.dto.FilterDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Filter DTO for querying audit log entries.
 *
 * <p>Extends the common {@link FilterDTO} with audit-specific filters: exact action, exact actor,
 * a timestamp range, and a free-text search matched against actor, action, and details.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter parameters for listing audit log entries")
public class AuditLogFilterDTO extends FilterDTO {

  @Schema(description = "Filter by exact action type", example = "LOGIN_SUCCESS")
  private AuditAction action;

  @Schema(description = "Filter by exact actor", example = "admin")
  private String actor;

  @Schema(description = "Only include entries at or after this timestamp", example = "2026-09-01T00:00:00")
  private LocalDateTime dateFrom;

  @Schema(description = "Only include entries at or before this timestamp", example = "2026-09-30T23:59:59")
  private LocalDateTime dateTo;

  @Schema(
      description = "Free-text search matching actor, action, or details (case-insensitive)",
      example = "login")
  private String search;

  /**
   * Constructs a filter with pagination/sorting parameters only.
   *
   * @param page the page number (zero-based)
   * @param size the page size
   * @param sort the sort property
   * @param order the sort order
   */
  public AuditLogFilterDTO(int page, int size, String sort, SortOrder order) {
    super(page, size, sort, order);
  }
}