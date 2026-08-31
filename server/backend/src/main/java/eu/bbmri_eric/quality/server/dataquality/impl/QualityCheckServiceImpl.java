package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.server.dataquality.domain.Category;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckVersion;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDetailedDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckUpdateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckVersionCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckVersionDTO;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service implementation for managing quality checks. */
@Service
@Transactional
class QualityCheckServiceImpl implements QualityCheckService {

  private final QualityCheckRepository qualityCheckRepository;
  private final CategoryRepository categoryRepository;
  private final ModelMapper modelMapper;

  public QualityCheckServiceImpl(
      QualityCheckRepository qualityCheckRepository,
      CategoryRepository categoryRepository,
      ModelMapper modelMapper) {
    this.qualityCheckRepository = qualityCheckRepository;
    this.categoryRepository = categoryRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public QualityCheckDetailedDTO findById(Long id) {
    QualityCheck qualityCheck =
        qualityCheckRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Quality check not found with ID: " + id));
    return modelMapper.map(qualityCheck, QualityCheckDetailedDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public List<QualityCheckDTO> findAll() {
    return qualityCheckRepository.findAll().stream()
        .map(qualityCheck -> modelMapper.map(qualityCheck, QualityCheckDTO.class))
        .toList();
  }

  @Override
  public QualityCheckDTO update(Long id, QualityCheckUpdateDTO updateDTO) {
    QualityCheck qualityCheck =
        qualityCheckRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Quality check not found with ID: " + id));
    qualityCheck.setName(updateDTO.getName());
    qualityCheck.setDescription(updateDTO.getDescription());
    qualityCheck.setWarningThreshold(updateDTO.getWarningThreshold());
    qualityCheck.setErrorThreshold(updateDTO.getErrorThreshold());
    setCategory(updateDTO, qualityCheck);
    return modelMapper.map(qualityCheckRepository.save(qualityCheck), QualityCheckDTO.class);
  }

  private void setCategory(QualityCheckUpdateDTO updateDTO, QualityCheck qualityCheck) {
    if (updateDTO.getCategoryId() != null) {
      Category category =
          categoryRepository
              .findById(updateDTO.getCategoryId())
              .orElseThrow(
                  () ->
                      new EntityNotFoundException(
                          "Category not found with ID: " + updateDTO.getCategoryId()));
      qualityCheck.setCategory(category);
    } else {
      qualityCheck.setCategory(null);
    }
  }

  @Override
  public QualityCheckDTO setKeywords(Long id, Set<String> keywords) {
    QualityCheck qualityCheck =
        qualityCheckRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Quality check not found with ID: " + id));

    qualityCheck.setKeywords(keywords);
    return modelMapper.map(qualityCheckRepository.save(qualityCheck), QualityCheckDTO.class);
  }

  @Override
  public QualityCheckVersionDTO createVersion(Long id, QualityCheckVersionCreateDTO createDTO) {
    QualityCheck qualityCheck =
        qualityCheckRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Quality check not found with ID: " + id));

    int version = resolveVersion(createDTO.getVersion(), qualityCheck);
    QualityCheckVersion qualityCheckVersion =
        new QualityCheckVersion(qualityCheck, version, createDTO.getQuery());
    qualityCheck.addVersion(qualityCheckVersion);
    qualityCheckRepository.save(qualityCheck);
    return modelMapper.map(qualityCheckVersion, QualityCheckVersionDTO.class);
  }

  private int resolveVersion(Integer requestedVersion, QualityCheck qualityCheck) {
    return requestedVersion != null
        ? requestedVersion
        : qualityCheck.getVersions().stream()
                .map(QualityCheckVersion::getVersion)
                .max(Comparator.naturalOrder())
                .orElse(0)
            + 1;
  }

  @Override
  @Transactional(readOnly = true)
  public List<QualityCheckVersionDTO> findVersions(Long id) {
    QualityCheck qualityCheck =
        qualityCheckRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Quality check not found with ID: " + id));
    return qualityCheck.getVersions().stream()
        .sorted(Comparator.comparingInt(QualityCheckVersion::getVersion))
        .map(version -> modelMapper.map(version, QualityCheckVersionDTO.class))
        .toList();
  }
}
