package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.common.dto.FilterDTO;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import eu.bbmri_eric.quality.agent.common.exception.EntityNotFoundException;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.domain.Category;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckFilterDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckUpdateDTO;
import eu.bbmri_eric.quality.agent.dataquality.exception.QualityCheckNotFoundException;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger logger = LoggerFactory.getLogger(QualityCheckServiceImpl.class);

  QualityCheckServiceImpl(
      QualityCheckRepository qualityCheckRepository,
      CategoryRepository categoryRepository,
      ModelMapper modelMapper) {
    this.qualityCheckRepository = qualityCheckRepository;
    this.categoryRepository = categoryRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  @Transactional
  public QualityCheckDTO create(QualityCheckCreateDTO createDTO) {
    QualityCheck qualityCheck = modelMapper.map(createDTO, QualityCheck.class);
    qualityCheck.setId(null);
    qualityCheck.setCategory(null);
    setCategory(createDTO.getCategoryId(), qualityCheck);
    qualityCheck = qualityCheckRepository.save(qualityCheck);
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
        QualityCheckSpecification.withCategoryName(filter.getCategoryName()), pageRequest);
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
  public QualityCheckDTO update(Long id, QualityCheckUpdateDTO updateDTO) {
    QualityCheck qualityCheck =
        qualityCheckRepository
            .findById(id)
            .orElseThrow(() -> new QualityCheckNotFoundException(id));
    modelMapper.map(updateDTO, qualityCheck);
    setCategory(updateDTO.getCategoryId(), qualityCheck);
    qualityCheck = qualityCheckRepository.save(qualityCheck);
    return modelMapper.map(qualityCheck, QualityCheckDTO.class);
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
  public void delete(Long id) {
    if (!qualityCheckRepository.existsById(id)) {
      throw new QualityCheckNotFoundException(id);
    }
    qualityCheckRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean exists(Long id) {
    return qualityCheckRepository.existsById(id);
  }
}
