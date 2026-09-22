package eu.bbmri_eric.quality.agent.audit;

import eu.bbmri_eric.quality.agent.audit.dto.AuditLogDTO;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogFilterDTO;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;

/**
 * Service interface for querying audit log entries.
 *
 * <p>Recording entries is intentionally not exposed here: other modules should not record entries
 * directly where an existing domain event (or {@code @Audited}) can be used instead, which keeps
 * auditing decoupled from business logic. The few places that do need to record directly (e.g.
 * {@code AuditAspect}) depend on the package-private {@code AuditRecorder} instead.
 */
public interface AuditService {

  /**
   * Retrieves audit log entries with pagination, sorting, filtering, and free-text search.
   *
   * @param filter the audit-specific filter
   * @return a page response containing filtered audit log entries
   */
  PageResponse<AuditLogDTO> findAll(AuditLogFilterDTO filter);
}
