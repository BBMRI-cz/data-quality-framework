package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckUpdateDTO;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service implementation for managing quality checks. */
@Service
@Transactional
class QualityCheckServiceImpl implements QualityCheckService {

  private final QualityCheckRepository qualityCheckRepository;
  private final ModelMapper modelMapper;

  public QualityCheckServiceImpl(
      QualityCheckRepository qualityCheckRepository, ModelMapper modelMapper) {
    this.qualityCheckRepository = qualityCheckRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public QualityCheckDTO findById(String id) {
    QualityCheck qualityCheck =
        qualityCheckRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Quality check not found with ID: " + id));
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
  public QualityCheckDTO update(String id, QualityCheckUpdateDTO updateDTO) {
    QualityCheck qualityCheck =
        qualityCheckRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Quality check not found with ID: " + id));
    qualityCheck.setName(updateDTO.getName());
    qualityCheck.setDescription(updateDTO.getDescription());
    qualityCheck.setWarningThreshold(updateDTO.getWarningThreshold());
    qualityCheck.setErrorThreshold(updateDTO.getErrorThreshold());
    QualityCheck updatedQualityCheck = qualityCheckRepository.save(qualityCheck);
    return modelMapper.map(updatedQualityCheck, QualityCheckDTO.class);
  }
}
