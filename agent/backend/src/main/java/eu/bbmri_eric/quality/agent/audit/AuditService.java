package eu.bbmri_eric.quality.agent.audit;

import eu.bbmri_eric.quality.agent.audit.domain.AuditAction;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogDTO;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogFilterDTO;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;

/**
 * Service interface for recording and querying audit log entries.
 *
 * <p>Other modules should not depend on this interface directly to record entries where an
 * existing domain event can be listened to instead; this keeps auditing decoupled from business
 * logic. Use {@link #record} directly only where no suitable event already exists (e.g.
 * authentication attempts).
 */
public interface AuditService {

  /**
   * Records an audit log entry.
   *
   * @param action the type of action performed
   * @param actor the username of the actor who performed the action, or {@code null} if the
   *     action was performed by the system
   * @param details free-text details describing the action
   */
  void record(AuditAction action, String actor, String details);

  /**
   * Records an audit log entry with module and entity context.
   *
   * @param action the type of action performed
   * @param actor the username of the actor who performed the action, or {@code null} if the
   *     action was performed by the system
   * @param details free-text details describing the action
   * @param module the module in which the action occurred, or {@code null}
   * @param entityId the ID of the entity affected by the action, or {@code null}
   */
  void record(AuditAction action, String actor, String details, String module, Long entityId);

  /**
   * Retrieves audit log entries with pagination, sorting, filtering, and free-text search.
   *
   * @param filter the audit-specific filter
   * @return a page response containing filtered audit log entries
   */
  PageResponse<AuditLogDTO> findAll(AuditLogFilterDTO filter);
}