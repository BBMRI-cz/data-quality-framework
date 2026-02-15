package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.common.dto.FilterDTO;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckUpdateDTO;
import eu.bbmri_eric.quality.agent.dataquality.exception.QualityCheckNotFoundException;
import java.util.List;
import org.modelmapper.ModelMapper;
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
  private final ModelMapper modelMapper;

  QualityCheckServiceImpl(QualityCheckRepository qualityCheckRepository, ModelMapper modelMapper) {
    this.qualityCheckRepository = qualityCheckRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  @Transactional
  public QualityCheckDTO create(QualityCheckCreateDTO createDTO) {
    QualityCheck qualityCheck = modelMapper.map(createDTO, QualityCheck.class);
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
    PageRequest pageRequest = PageRequest.of(filter.getPage(), filter.getSize(), sort);
    Page<QualityCheck> page = qualityCheckRepository.findAll(pageRequest);

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

    if (updateDTO.getName() != null) {
      qualityCheck.setName(updateDTO.getName());
    }
    if (updateDTO.getDescription() != null) {
      qualityCheck.setDescription(updateDTO.getDescription());
    }
    if (updateDTO.getQuery() != null) {
      qualityCheck.setQuery(updateDTO.getQuery());
    }
    if (updateDTO.getWarningThreshold() != null) {
      qualityCheck.setWarningThreshold(updateDTO.getWarningThreshold());
    }
    if (updateDTO.getErrorThreshold() != null) {
      qualityCheck.setErrorThreshold(updateDTO.getErrorThreshold());
    }
    if (updateDTO.getEpsilonBudget() != null) {
      qualityCheck.setEpsilonBudget(updateDTO.getEpsilonBudget());
    }

    qualityCheck = qualityCheckRepository.save(qualityCheck);
    return modelMapper.map(qualityCheck, QualityCheckDTO.class);
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
