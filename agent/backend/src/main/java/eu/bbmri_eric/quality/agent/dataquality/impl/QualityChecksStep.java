package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.DataStoreFactory;
import eu.bbmri_eric.quality.agent.dataquality.FHIRServer;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.DataQualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheckType;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import eu.bbmri_eric.quality.agent.dataquality.impl.store.OmopDataStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class QualityChecksStep implements ReportPipelineStep {

  private final QualityCheckRepository repository;
  private final DataStoreFactory dataStoreFactory;
  private final ModelMapper modelMapper;

  QualityChecksStep(
      QualityCheckRepository repository,
      DataStoreFactory dataStoreFactory,
      ModelMapper modelMapper) {
    this.repository = repository;
    this.dataStoreFactory = dataStoreFactory;
    this.modelMapper = modelMapper;
  }

  @Override
  public Report execute(Report report) {
    log.info("Running quality checks for report id: {}", report.getId());
    DataStore dataStore = dataStoreFactory.resolveDataStore();
    List<DataQualityCheck> dataQualityChecks = compileChecksToRun(dataStore);
    runRelevantChecks(report, dataQualityChecks, dataStore);
    log.info("Completed quality checks for report id: {}", report.getId());
    return report;
  }

  private void runRelevantChecks(
      Report report, List<DataQualityCheck> dataQualityChecks, DataStore dataStore) {
    for (DataQualityCheck dataQualityCheck : dataQualityChecks) {
      executeCheck(dataQualityCheck, report, dataStore);
    }
  }

  private @NonNull List<DataQualityCheck> compileChecksToRun(DataStore dataStore) {
    List<DataQualityCheck> dataQualityChecks = new ArrayList<>();
    for (QualityCheck qualityCheck : repository.findAll()) {
      if (qualityCheck.getType() == QualityCheckType.CQL && dataStore instanceof FHIRServer) {
        dataQualityChecks.add(qualityCheck);
      } else if (qualityCheck.getType() == QualityCheckType.JAVA
          && dataStore instanceof FHIRServer) {
        createBuiltInCheck(qualityCheck).ifPresent(dataQualityChecks::add);
      } else if (qualityCheck.getType() == QualityCheckType.SQL
          && dataStore instanceof OmopDataStore) {
        dataQualityChecks.add(qualityCheck);
      }
    }
    return dataQualityChecks;
  }

  private Optional<DataQualityCheck> createBuiltInCheck(QualityCheck config) {
    Long id = config.getId();
    if (DuplicateIdentifierCheck.CHECK_ID.equals(id)) {
      return Optional.of(new DuplicateIdentifierCheck(config));
    } else if (InvalidConditionICDCheck.CHECK_ID.equals(id)) {
      return Optional.of(new InvalidConditionICDCheck(config));
    } else if (UpdateCheck.CHECK_ID.equals(id)) {
      return Optional.of(new UpdateCheck(config));
    }
    log.warn("Unknown built-in check with id: {}", id);
    return Optional.empty();
  }

  private void executeCheck(DataQualityCheck check, Report report, DataStore dataStore) {
    ResultDTO resultDTO = check.execute(dataStore);
    Result result = modelMapper.map(resultDTO, Result.class);
    modelMapper.map(check, result);
    report.addResult(result);
  }

  @Override
  public int getOrder() {
    return 10;
  }
}
