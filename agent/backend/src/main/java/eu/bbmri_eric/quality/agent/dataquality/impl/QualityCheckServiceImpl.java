package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.dataquality.exception.CQLQueryNotFoundException;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service implementation for managing CQL queries. */
@Service
@Transactional
class QualityCheckServiceImpl implements QualityCheckService {

  private final QualityCheckRepository cqlCheckRepository;
  private final ModelMapper modelMapper;

  QualityCheckServiceImpl(QualityCheckRepository cqlCheckRepository, ModelMapper modelMapper) {
    this.cqlCheckRepository = cqlCheckRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public QualityCheckDTO findById(Long id) {
    QualityCheck cqlQuery =
        cqlCheckRepository.findById(id).orElseThrow(() -> new CQLQueryNotFoundException(id));
    return modelMapper.map(cqlQuery, QualityCheckDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public List<QualityCheckDTO> findAll() {
    return cqlCheckRepository.findAll().stream()
        .map(cqlQuery -> modelMapper.map(cqlQuery, QualityCheckDTO.class))
        .toList();
  }
}
