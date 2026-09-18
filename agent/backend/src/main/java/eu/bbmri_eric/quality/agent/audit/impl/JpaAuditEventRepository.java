package eu.bbmri_eric.quality.agent.audit.impl;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.stereotype.Component;

/**
 * Persists Spring Boot {@link AuditEvent}s (both our own, published via {@code AuditService}, and
 * ones Spring Security publishes automatically, e.g. authentication success/failure) into the
 * existing {@code audit_log} table.
 *
 * <p>Registering this bean makes Spring Boot's {@code AuditAutoConfiguration} back off from
 * creating its default in-memory repository and wire an {@code AuditListener} that forwards every
 * published {@code AuditApplicationEvent} here.
 */
@Component
class JpaAuditEventRepository implements AuditEventRepository {

  private static final Logger logger = LoggerFactory.getLogger(JpaAuditEventRepository.class);

  private final AuditLogRepository auditLogRepository;

  JpaAuditEventRepository(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Override
  public void add(AuditEvent event) {
    AuditLogEntry entry = new AuditLogEntry();
    entry.setTimestamp(LocalDateTime.ofInstant(event.getTimestamp(), ZoneOffset.UTC));
    entry.setActor(event.getPrincipal());
    entry.setAction(mapType(event.getType()));

    Map<String, Object> data = event.getData();
    Object details = data.get("details");
    entry.setDetails(details != null ? details.toString() : null);
    Object module = data.get("module");
    entry.setModule(module != null ? module.toString() : null);
    Object entityId = data.get("entityId");
    if (entityId instanceof Number number) {
      entry.setEntityId(number.longValue());
    }
    Object actorId = data.get("actorId");
    if (actorId instanceof Number number) {
      entry.setActorId(number.longValue());
    }

    auditLogRepository.save(entry);
  }

  @Override
  public List<AuditEvent> find(String principal, Instant after, String type) {
    AuditAction action = type != null ? mapType(type) : null;
    return auditLogRepository
        .findAll(AuditLogSpecification.forAuditQuery(principal, after, action))
        .stream()
        .map(JpaAuditEventRepository::toAuditEvent)
        .toList();
  }

  private AuditAction mapType(String type) {
    return switch (type) {
      case "AUTHENTICATION_SUCCESS" -> AuditAction.LOGIN_SUCCESS;
      case "AUTHENTICATION_FAILURE" -> AuditAction.LOGIN_FAILURE;
      default -> {
        try {
          yield AuditAction.valueOf(type);
        } catch (IllegalArgumentException ex) {
          logger.debug("No AuditAction mapping for audit event type '{}', using OTHER", type);
          yield AuditAction.OTHER;
        }
      }
    };
  }

  private static AuditEvent toAuditEvent(AuditLogEntry entry) {
    Map<String, Object> data = new LinkedHashMap<>();
    if (entry.getDetails() != null) {
      data.put("details", entry.getDetails());
    }
    if (entry.getModule() != null) {
      data.put("module", entry.getModule());
    }
    if (entry.getEntityId() != null) {
      data.put("entityId", entry.getEntityId());
    }
    if (entry.getActorId() != null) {
      data.put("actorId", entry.getActorId());
    }
    String type = entry.getAction() != null ? entry.getAction().name() : AuditAction.OTHER.name();
    return new AuditEvent(
        entry.getTimestamp().toInstant(ZoneOffset.UTC), entry.getActor(), type, data);
  }
}
