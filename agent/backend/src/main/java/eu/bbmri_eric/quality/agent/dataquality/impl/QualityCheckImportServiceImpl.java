package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.Audited;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckImportService;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckType;
import eu.bbmri_eric.quality.agent.dataquality.domain.Category;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckImportSummaryDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckTransferDTO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Imports quality checks from a file in the portable export format. */
@Service
class QualityCheckImportServiceImpl implements QualityCheckImportService {

  private static final Logger logger = LoggerFactory.getLogger(QualityCheckImportServiceImpl.class);

  private final QualityCheckRepository qualityCheckRepository;
  private final CategoryRepository categoryRepository;
  private final QualityCheckImportValidator validator;
  private final ModelMapper modelMapper;

  QualityCheckImportServiceImpl(
      QualityCheckRepository qualityCheckRepository,
      CategoryRepository categoryRepository,
      QualityCheckImportValidator validator,
      ModelMapper modelMapper) {
    this.qualityCheckRepository = qualityCheckRepository;
    this.categoryRepository = categoryRepository;
    this.validator = validator;
    this.modelMapper = modelMapper;
    // The file carries the category by name; it is resolved to an entity separately.
    if (modelMapper.getTypeMap(QualityCheckTransferDTO.class, QualityCheck.class) == null) {
      modelMapper
          .emptyTypeMap(QualityCheckTransferDTO.class, QualityCheck.class)
          .addMappings(mapping -> mapping.skip(QualityCheck::setCategory))
          .implicitMappings();
    }
  }

  @Override
  @Transactional
  @Audited(
      action = AuditAction.QUALITY_CHECKS_IMPORTED,
      module = "dataquality",
      details = "Quality checks imported")
  public QualityCheckImportSummaryDTO importChecks(List<QualityCheckTransferDTO> entries) {
    entries.forEach(QualityCheckImportServiceImpl::applyDefaults);
    Map<String, List<QualityCheck>> existingByName =
        qualityCheckRepository.findAll().stream()
            .collect(Collectors.groupingBy(QualityCheck::getName));
    validator.validate(entries, existingByName);

    Map<String, Category> categories = new HashMap<>();
    int created = 0;
    int updated = 0;
    int unchanged = 0;
    for (QualityCheckTransferDTO entry : entries) {
      List<QualityCheck> matches = existingByName.get(entry.getName());
      if (matches == null) {
        save(entry, modelMapper.map(entry, QualityCheck.class), categories);
        created++;
        continue;
      }
      QualityCheck existing = matches.getFirst();
      if (entry.getType() == QualityCheckType.JAVA) {
        // A built-in check's query is implemented in the agent and never imported.
        entry.setQuery(existing.getQuery());
      }
      if (entry.equals(toTransfer(existing))) {
        unchanged++;
      } else {
        modelMapper.map(entry, existing);
        save(entry, existing, categories);
        updated++;
      }
    }
    logger.info(
        "Imported {} quality checks: {} created, {} updated, {} unchanged",
        entries.size(),
        created,
        updated,
        unchanged);
    return new QualityCheckImportSummaryDTO(created, updated, unchanged);
  }

  /** A missing type means CQL, like when creating a check; a blank category means none. */
  private static void applyDefaults(QualityCheckTransferDTO entry) {
    if (entry.getType() == null) {
      entry.setType(QualityCheckType.CQL);
    }
    if (entry.getCategory() != null && entry.getCategory().isBlank()) {
      entry.setCategory(null);
    }
  }

  private QualityCheckTransferDTO toTransfer(QualityCheck check) {
    return QualityCheckTransferDTO.from(modelMapper.map(check, QualityCheckDTO.class));
  }

  private void save(
      QualityCheckTransferDTO entry, QualityCheck check, Map<String, Category> categories) {
    check.setCategory(
        entry.getCategory() == null
            ? null
            : categories.computeIfAbsent(entry.getCategory(), this::findOrCreateCategory));
    qualityCheckRepository.save(check);
  }

  /** Finds the category with the given name, creating it without a colour when missing. */
  private Category findOrCreateCategory(String name) {
    return categoryRepository
        .findByName(name)
        .orElseGet(() -> categoryRepository.save(new Category(name, null)));
  }
}
