package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.dataquality.exception.QualityCheckNotFoundException;
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

  QualityCheckServiceImpl(QualityCheckRepository qualityCheckRepository, ModelMapper modelMapper) {
    this.qualityCheckRepository = qualityCheckRepository;
    this.modelMapper = modelMapper;
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
}
