package eu.bbmri_eric.quality.agent.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.domain.AuditLogEntry;
import eu.bbmri_eric.quality.agent.dataquality.CategoryService;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckType;
import eu.bbmri_eric.quality.agent.dataquality.ReportService;
import eu.bbmri_eric.quality.agent.dataquality.dto.CategoryCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.CategoryUpdateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckBulkUpdateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckUpdateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportCreateDTO;
import jakarta.transaction.Transactional;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;

/**
 * Verifies that changes to categories, quality checks and reports produce audit log entries via
 * {@code @Audited} on the dataquality services.
 */
@SpringBootTest
@Transactional
@WithUserDetails("admin")
class DataQualityAuditIntegrationTest {

  private static final String DATAQUALITY_MODULE = "dataquality";
  private static final String ADMIN_USER = "admin";

  @Autowired private AuditLogRepository auditLogRepository;
  @Autowired private CategoryService categoryService;
  @Autowired private QualityCheckService qualityCheckService;
  @Autowired private ReportService reportService;

  @BeforeEach
  void setUp() {
    auditLogRepository.deleteAll();
  }

  @Test
  void categoryLifecycle_recordsCreatedUpdatedAndDeletedAuditEntries() {
    Long id = categoryService.create(new CategoryCreateDTO("Completeness", "#FF5733")).getId();
    categoryService.update(id, new CategoryUpdateDTO("Accuracy", "#33FF57"));
    categoryService.delete(id);

    assertRecorded(AuditAction.QUALITY_CHECK_CATEGORY_CREATED, id, "Category created");
    assertRecorded(AuditAction.QUALITY_CHECK_CATEGORY_UPDATED, id, "Category updated");
    assertRecorded(AuditAction.QUALITY_CHECK_CATEGORY_DELETED, id, "Category deleted");
  }

  @Test
  void qualityCheckLifecycle_recordsCreatedUpdatedAndDeletedAuditEntries() {
    Long id =
        qualityCheckService
            .create(
                new QualityCheckCreateDTO(
                    "Age check", "desc", "query", QualityCheckType.CQL, 10, 30, 1.0))
            .getId();
    qualityCheckService.update(
        id,
        new QualityCheckUpdateDTO(
            "Age check v2", "desc", "query", QualityCheckType.CQL, 10, 30, 1.0));
    qualityCheckService.delete(id);

    assertRecorded(AuditAction.QUALITY_CHECK_CREATED, id, "Quality check created");
    assertRecorded(AuditAction.QUALITY_CHECK_UPDATED, id, "Quality check updated");
    assertRecorded(AuditAction.QUALITY_CHECK_DELETED, id, "Quality check deleted");
  }

  @Test
  void updateAll_recordsBulkUpdateAuditEntry() {
    Long id =
        qualityCheckService
            .create(
                new QualityCheckCreateDTO(
                    "Age check", "desc", "query", QualityCheckType.CQL, 10, 30, 1.0))
            .getId();
    QualityCheckBulkUpdateDTO updateDTO = new QualityCheckBulkUpdateDTO();
    updateDTO.setId(id);
    updateDTO.setName("Age check v2");

    qualityCheckService.updateAll(List.of(updateDTO));

    assertRecorded(AuditAction.QUALITY_CHECK_UPDATED, null, "Quality checks bulk updated");
  }

  @Test
  void createReport_recordsReportCreatedAuditEntry() {
    Long id = reportService.create(new ReportCreateDTO()).getId();

    assertRecorded(AuditAction.REPORT_CREATED, id, "Report created");
  }

  private void assertRecorded(AuditAction action, Long entityId, String details) {
    List<AuditLogEntry> entries = auditLogRepository.findAll();
    assertThat(entries)
        .anySatisfy(
            entry -> {
              assertThat(entry.getAction()).isEqualTo(action);
              assertThat(entry.getActor()).isEqualTo(ADMIN_USER);
              assertThat(entry.getModule()).isEqualTo(DATAQUALITY_MODULE);
              assertThat(entry.getEntityId()).isEqualTo(entityId);
              assertThat(entry.getDetails()).isEqualTo(details);
            });
  }
}
