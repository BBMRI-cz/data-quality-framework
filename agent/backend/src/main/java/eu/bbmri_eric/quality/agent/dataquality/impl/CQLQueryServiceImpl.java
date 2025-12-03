package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.CQLQueryService;
import eu.bbmri_eric.quality.agent.dataquality.domain.CQLQuery;
import eu.bbmri_eric.quality.agent.dataquality.dto.CQLQueryDTO;
import eu.bbmri_eric.quality.agent.dataquality.exception.CQLQueryNotFoundException;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service implementation for managing CQL queries. */
@Service
@Transactional
class CQLQueryServiceImpl implements CQLQueryService {

  private final CQLCheckRepository cqlCheckRepository;
  private final ModelMapper modelMapper;

  CQLQueryServiceImpl(CQLCheckRepository cqlCheckRepository, ModelMapper modelMapper) {
    this.cqlCheckRepository = cqlCheckRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public CQLQueryDTO findById(Long id) {
    CQLQuery cqlQuery =
        cqlCheckRepository.findById(id).orElseThrow(() -> new CQLQueryNotFoundException(id));
    return modelMapper.map(cqlQuery, CQLQueryDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CQLQueryDTO> findAll() {
    return cqlCheckRepository.findAll().stream()
        .map(cqlQuery -> modelMapper.map(cqlQuery, CQLQueryDTO.class))
        .toList();
  }
}
