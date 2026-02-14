package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.FHIRStore;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.DataQualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheckType;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class QualityChecksStep implements ReportPipelineStep {

  private final QualityCheckRepository repository;
  private final FHIRStore fhirStore;

  QualityChecksStep(QualityCheckRepository repository, FHIRStore fhirStore) {
    this.repository = repository;
    this.fhirStore = fhirStore;
  }

  @Override
  public Report execute(Report report) {
    log.info("Running quality checks for report id: {}", report.getId());

    List<DataQualityCheck> dataQualityChecks = new ArrayList<>();

    for (QualityCheck qualityCheck : repository.findAll()) {
      if (qualityCheck.getType() == QualityCheckType.CQL) {
        dataQualityChecks.add(qualityCheck);
      } else if (qualityCheck.getType() == QualityCheckType.JAVA) {
        createBuiltInCheck(qualityCheck).ifPresent(dataQualityChecks::add);
      }
    }

    for (DataQualityCheck dataQualityCheck : dataQualityChecks) {
      if (dataQualityCheck instanceof StratifiedDataQualityCheck stratifiedCheck) {
        executeStratifiedCheck(stratifiedCheck, report);
      } else {
        executeCheck(dataQualityCheck, report);
      }
    }

    log.info("Completed quality checks for report id: {}", report.getId());
    return report;
  }

  private Optional<DataQualityCheck> createBuiltInCheck(QualityCheck config) {
    Long id = config.getId();
    if (DuplicateIdentifierCheck.CHECK_ID.equals(id)) {
      return Optional.of(new DuplicateIdentifierCheck(config));
    } else if (InvalidConditionICDCheck.CHECK_ID.equals(id)) {
      return Optional.of(new InvalidConditionICDCheck(config));
    } else if (SurvivalRateCheck.CHECK_ID.equals(id)) {
      return Optional.of(new SurvivalRateCheck(config));
    } else if (UpdateCheck.CHECK_ID.equals(id)) {
      return Optional.of(new UpdateCheck(config));
    }
    log.warn("Unknown built-in check with id: {}", id);
    return Optional.empty();
  }

  private void executeStratifiedCheck(StratifiedDataQualityCheck check, Report report) {
    Map<String, ResultDTO> results = check.executeWithStratification(fhirStore);
    int count = results.size();

    for (Map.Entry<String, ResultDTO> entry : results.entrySet()) {
      String stratum = entry.getKey();
      ResultDTO resultDTO = entry.getValue();

      Result result =
          new Result(
              check.getName() + " (%s)".formatted(stratum),
              check.getId(),
              resultDTO.rawResult(),
              0.0,
              check.getWarningThreshold(),
              check.getErrorThreshold(),
              check.getEpsilonBudget() / count,
              resultDTO.error(),
              stratum);
      result.setPatients(resultDTO.idSet());
      report.addResult(result);
    }
  }

  private void executeCheck(DataQualityCheck check, Report report) {
    ResultDTO resultDTO = check.execute(fhirStore);

    Result result =
        new Result(
            check.getName(),
            check.getId(),
            resultDTO.rawResult(),
            0.0,
            check.getWarningThreshold(),
            check.getErrorThreshold(),
            check.getEpsilonBudget(),
            resultDTO.error(),
            null);
    result.setPatients(resultDTO.idSet());
    report.addResult(result);
  }

  @Override
  public int getOrder() {
    return 10;
  }
}
