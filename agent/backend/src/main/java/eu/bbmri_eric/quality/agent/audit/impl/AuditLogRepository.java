package eu.bbmri_eric.quality.agent.audit.impl;

import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for managing {@link AuditLogEntry} entities. */
@Repository
interface AuditLogRepository
    extends JpaRepository<AuditLogEntry, Long>, JpaSpecificationExecutor<AuditLogEntry> {}
