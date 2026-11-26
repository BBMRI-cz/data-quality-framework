package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.audit.AuditAction;
import eu.bbmri_eric.quality.agent.audit.Audited;
import eu.bbmri_eric.quality.agent.common.dto.FilterDTO;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import eu.bbmri_eric.quality.agent.common.exception.EntityNotFoundException;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckType;
import eu.bbmri_eric.quality.agent.dataquality.domain.Category;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckBulkUpdateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckFilterDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckImportDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckImportSummaryDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckUpdateDTO;
import eu.bbmri_eric.quality.agent.dataquality.exception.QualityCheckImportValidationException;
import eu.bbmri_eric.quality.agent.dataquality.exception.QualityCheckImportValidationException.EntryError;
import eu.bbmri_eric.quality.agent.dataquality.exception.QualityCheckNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service implementation for managing quality checks. */
@Service
@Transactional
class QualityCheckServiceImpl implements QualityCheckService {

  private final QualityCheckRepository qualityCheckRepository;
  private final CategoryRepository categoryRepository;
  private final ModelMapper modelMapper;

  /**
   * This bean's own Spring proxy. Calls via {@code this} bypass the proxy, so aspect
   * {@code @Audited} on {@link #updatePartially} would not run; calling through {@code self} makes
   * them apply.
   */
  private final QualityCheckServiceImpl self;

  private static final Logger logger = LoggerFactory.getLogger(QualityCheckServiceImpl.class);

  QualityCheckServiceImpl(
      QualityCheckRepository qualityCheckRepository,
      CategoryRepository categoryRepository,
      ModelMapper modelMapper,
      @Lazy QualityCheckServiceImpl self) {
    this.qualityCheckRepository = qualityCheckRepository;
    this.categoryRepository = categoryRepository;
    this.modelMapper = modelMapper;
    this.self = self;
  }

  @Override
  @Transactional
  @Audited(
      action = AuditAction.QUALITY_CHECK_CREATED,
      module = "dataquality",
      entityId = "#result.id",
      details = "Quality check created")
  public QualityCheckDTO create(QualityCheckCreateDTO createDTO) {
    QualityCheck qualityCheck = modelMapper.map(createDTO, QualityCheck.class);
    qualityCheck.setId(null);
    qualityCheck.setCategory(null);
    setCategory(createDTO.getCategoryId(), qualityCheck);
    qualityCheck = qualityCheckRepository.save(qualityCheck);
    logger.info("Created quality check id: {}", qualityCheck.getId());
    return modelMapper.map(qualityCheck, QualityCheckDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public QualityCheckDTO findById(Long id) {
    QualityCheck qualityCheck =
        qualityCheckRepository
            .findById(id)
            .orElseThrow(() -> new QualityCheckNotFoundException(id));
    return modelMapper.map(qualityCheck, QualityCheckDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public List<QualityCheckDTO> findAll() {
    return qualityCheckRepository.findAll().stream()
        .map(qualityCheck -> modelMapper.map(qualityCheck, QualityCheckDTO.class))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<QualityCheckDTO> findAll(FilterDTO filter) {
    if (filter instanceof QualityCheckFilterDTO qualityCheckFilter) {
      return findAll(qualityCheckFilter);
    }
    return findAllInternal(filter, qualityCheckRepository::findAll);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<QualityCheckDTO> findAll(QualityCheckFilterDTO filter) {
    PageRequest pageRequest = createPageRequest(filter);
    Page<QualityCheck> page = fetchFilteredPage(filter, pageRequest);
    return mapToPageResponse(page);
  }

  private PageRequest createPageRequest(FilterDTO filter) {
    Sort.Direction direction =
        filter.getOrder() == null || filter.getOrder().name().equalsIgnoreCase("ASC")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

    String sortProperty = filter.getSort();
    if (sortProperty == null) {
      sortProperty = "id";
      direction = Sort.Direction.DESC;
    }

    Sort sort = Sort.by(direction, sortProperty);
    return PageRequest.of(filter.getPage(), filter.getSize(), sort);
  }

  private Page<QualityCheck> fetchFilteredPage(
      QualityCheckFilterDTO filter, PageRequest pageRequest) {
    return qualityCheckRepository.findAll(
        QualityCheckSpecification.withCategoryNames(filter.getCategoryName()), pageRequest);
  }

  private PageResponse<QualityCheckDTO> findAllInternal(
      FilterDTO filter, java.util.function.Function<PageRequest, Page<QualityCheck>> fetcher) {
    PageRequest pageRequest = createPageRequest(filter);
    Page<QualityCheck> page = fetcher.apply(pageRequest);
    return mapToPageResponse(page);
  }

  private PageResponse<QualityCheckDTO> mapToPageResponse(Page<QualityCheck> page) {
    List<QualityCheckDTO> content =
        page.getContent().stream()
            .map(qualityCheck -> modelMapper.map(qualityCheck, QualityCheckDTO.class))
            .toList();

    return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
  }

  @Override
  @Transactional
  @Audited(
      action = AuditAction.QUALITY_CHECK_UPDATED,
      module = "dataquality",
      entityId = "#id",
      details = "Quality check updated",
      diff = "#target.findById(#id)",
      sensitive = "query")
  public QualityCheckDTO update(Long id, QualityCheckUpdateDTO updateDTO) {
    QualityCheck qualityCheck =
        qualityCheckRepository
            .findById(id)
            .orElseThrow(() -> new QualityCheckNotFoundException(id));
    modelMapper.map(updateDTO, qualityCheck);
    qualityCheck.setEpsilonBudget(updateDTO.getEpsilonBudget());
    setCategory(updateDTO.getCategoryId(), qualityCheck);
    qualityCheck = qualityCheckRepository.save(qualityCheck);
    logger.info("Updated quality check id: {}", id);
    return modelMapper.map(qualityCheck, QualityCheckDTO.class);
  }

  @Override
  @Transactional
  public List<QualityCheckDTO> updateAll(List<QualityCheckBulkUpdateDTO> updateDTOs) {
    List<QualityCheckDTO> updated = updateDTOs.stream().map(self::updatePartially).toList();
    logger.info("Bulk updated {} quality checks", updated.size());
    return updated;
  }

  /**
   * Applies a partial update to a single quality check. Unlike the full replace in {@link
   * #update(Long, QualityCheckUpdateDTO)}, a {@code null} category ID leaves the assigned category
   * untouched instead of clearing it. Public and called via {@code self} so each update is audited.
   */
  @Audited(
      action = AuditAction.QUALITY_CHECK_UPDATED,
      module = "dataquality",
      entityId = "#updateDTO.id",
      details = "Quality check bulk updated",
      diff = "#target.findById(#updateDTO.id)",
      sensitive = "query")
  public QualityCheckDTO updatePartially(QualityCheckBulkUpdateDTO updateDTO) {
    QualityCheck qualityCheck =
        qualityCheckRepository
            .findById(updateDTO.getId())
            .orElseThrow(() -> new QualityCheckNotFoundException(updateDTO.getId()));
    modelMapper.map(updateDTO, qualityCheck);
    if (updateDTO.getCategoryId() != null) {
      setCategory(updateDTO.getCategoryId(), qualityCheck);
    }
    qualityCheck = qualityCheckRepository.save(qualityCheck);
    return modelMapper.map(qualityCheck, QualityCheckDTO.class);
  }

  @Override
  @Transactional
  @Audited(
      action = AuditAction.QUALITY_CHECKS_IMPORTED,
      module = "dataquality",
      details = "Quality checks imported")
  public QualityCheckImportSummaryDTO importChecks(List<QualityCheckImportDTO> entries) {
    Map<String, List<QualityCheck>> existingByName =
        qualityCheckRepository.findAll().stream()
            .collect(Collectors.groupingBy(QualityCheck::getName));
    List<EntryError> errors = collectImportErrors(entries, existingByName);
    if (!errors.isEmpty()) {
      throw new QualityCheckImportValidationException(errors);
    }

    Map<String, Category> categoriesByName = new HashMap<>();
    int created = 0;
    int updated = 0;
    int unchanged = 0;
    for (QualityCheckImportDTO entry : entries) {
      QualityCheckType type = importedType(entry);
      Category category = resolveImportedCategory(entry.getCategory(), categoriesByName);
      List<QualityCheck> matches = existingByName.get(entry.getName());
      if (matches == null) {
        qualityCheckRepository.save(newImportedCheck(entry, type, category));
        created++;
      } else if (applyImportedValues(matches.getFirst(), entry, type, category)) {
        updated++;
      } else {
        unchanged++;
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

  /** Validates every import entry up front; an empty list means the whole file can be applied. */
  private List<EntryError> collectImportErrors(
      List<QualityCheckImportDTO> entries, Map<String, List<QualityCheck>> existingByName) {
    Map<String, Long> fileOccurrences =
        entries.stream()
            .collect(Collectors.groupingBy(QualityCheckImportDTO::getName, Collectors.counting()));
    List<EntryError> errors = new ArrayList<>();
    for (int i = 0; i < entries.size(); i++) {
      QualityCheckImportDTO entry = entries.get(i);
      int position = i + 1;
      QualityCheckType type = importedType(entry);
      if (fileOccurrences.get(entry.getName()) > 1) {
        addImportError(errors, position, entry, "the name appears more than once in the file");
        continue;
      }
      List<QualityCheck> matches = existingByName.get(entry.getName());
      if (matches != null && matches.size() > 1) {
        addImportError(
            errors,
            position,
            entry,
            "multiple existing checks are named '" + entry.getName() + "'");
      } else if (type == QualityCheckType.JAVA) {
        boolean builtinExists =
            matches != null && matches.getFirst().getType() == QualityCheckType.JAVA;
        if (!builtinExists) {
          addImportError(
              errors,
              position,
              entry,
              "no existing built-in check named '"
                  + entry.getName()
                  + "'; JAVA checks can only be updated, never created");
        }
      } else if (matches != null && matches.getFirst().getType() == QualityCheckType.JAVA) {
        addImportError(
            errors,
            position,
            entry,
            "'" + entry.getName() + "' is a built-in check; its type cannot be changed to " + type);
      } else if (entry.getQuery() == null || entry.getQuery().isBlank()) {
        addImportError(errors, position, entry, "query is required for type " + type);
      }
    }
    return errors;
  }

  private void addImportError(
      List<EntryError> errors, int position, QualityCheckImportDTO entry, String reason) {
    errors.add(new EntryError(position, entry.getName(), reason));
  }

  /** Resolves the type of an import entry, defaulting to CQL like the create payload does. */
  private static QualityCheckType importedType(QualityCheckImportDTO entry) {
    return entry.getType() == null ? QualityCheckType.CQL : entry.getType();
  }

  /**
   * Finds the category with the given name, creating it without a color when missing. Blank names
   * mean uncategorized.
   */
  private Category resolveImportedCategory(
      String categoryName, Map<String, Category> categoriesByName) {
    if (categoryName == null || categoryName.isBlank()) {
      return null;
    }
    return categoriesByName.computeIfAbsent(
        categoryName,
        name ->
            categoryRepository
                .findByName(name)
                .orElseGet(() -> categoryRepository.save(new Category(name, null))));
  }

  private QualityCheck newImportedCheck(
      QualityCheckImportDTO entry, QualityCheckType type, Category category) {
    QualityCheck qualityCheck =
        new QualityCheck(entry.getName(), entry.getDescription(), entry.getQuery());
    qualityCheck.setType(type);
    qualityCheck.setWarningThreshold(entry.getWarningThreshold());
    qualityCheck.setErrorThreshold(entry.getErrorThreshold());
    qualityCheck.setEpsilonBudget(entry.getEpsilonBudget());
    qualityCheck.setCategory(category);
    return qualityCheck;
  }

  /**
   * Overwrites the imported fields of an existing check, but only those that actually differ; the
   * active flag is not part of the format and is never touched, and the query of a JAVA check is
   * implemented in the agent and never overwritten.
   *
   * @return true if the check was changed and saved, false if it was already up to date
   */
  private boolean applyImportedValues(
      QualityCheck existing,
      QualityCheckImportDTO entry,
      QualityCheckType type,
      Category category) {
    String existingCategoryName =
        existing.getCategory() == null ? null : existing.getCategory().getName();
    String importedCategoryName = category == null ? null : category.getName();
    boolean queryDiffers =
        type != QualityCheckType.JAVA && !Objects.equals(existing.getQuery(), entry.getQuery());
    boolean unchanged =
        Objects.equals(existing.getDescription(), entry.getDescription())
            && existing.getType() == type
            && existing.getWarningThreshold() == entry.getWarningThreshold()
            && existing.getErrorThreshold() == entry.getErrorThreshold()
            && Objects.equals(existing.getEpsilonBudget(), entry.getEpsilonBudget())
            && Objects.equals(existingCategoryName, importedCategoryName)
            && !queryDiffers;
    if (unchanged) {
      return false;
    }
    existing.setDescription(entry.getDescription());
    existing.setType(type);
    existing.setWarningThreshold(entry.getWarningThreshold());
    existing.setErrorThreshold(entry.getErrorThreshold());
    existing.setEpsilonBudget(entry.getEpsilonBudget());
    existing.setCategory(category);
    if (queryDiffers) {
      existing.setQuery(entry.getQuery());
    }
    qualityCheckRepository.save(existing);
    return true;
  }

  private void setCategory(Long categoryId, QualityCheck qualityCheck) {
    if (categoryId == null) {
      qualityCheck.setCategory(null);
      return;
    }
    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(
                () -> new EntityNotFoundException("Category not found with ID: " + categoryId));
    qualityCheck.setCategory(category);
  }

  @Override
  @Transactional(readOnly = true)
  public long count() {
    return qualityCheckRepository.count();
  }

  @Override
  @Transactional
  @Audited(
      action = AuditAction.QUALITY_CHECK_DELETED,
      module = "dataquality",
      entityId = "#id",
      details = "Quality check deleted")
  public void delete(Long id) {
    if (!qualityCheckRepository.existsById(id)) {
      throw new QualityCheckNotFoundException(id);
    }
    qualityCheckRepository.deleteById(id);
    logger.info("Deleted quality check id: {}", id);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean exists(Long id) {
    return qualityCheckRepository.existsById(id);
  }
}
