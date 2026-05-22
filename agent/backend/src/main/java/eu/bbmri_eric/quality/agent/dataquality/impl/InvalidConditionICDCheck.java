package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.ICD10Validator;
import eu.bbmri_eric.ICDValidator;
import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.FHIRServer;
import eu.bbmri_eric.quality.agent.dataquality.domain.DataQualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Resource;

@Slf4j
class InvalidConditionICDCheck implements DataQualityCheck {
  static final Long CHECK_ID = 1001L;

  private final QualityCheck config;

  InvalidConditionICDCheck(QualityCheck config) {
    this.config = config;
  }

  @Override
  public ResultDTO execute(DataStore dataStore) {
    if (!(dataStore instanceof FHIRServer fhirStore)) {
      return new ResultDTO("FHIR data store required for " + getName());
    }
    ICDValidator icdValidator = new ICD10Validator();
    try {
      List<Resource> conditions =
          fhirStore.fetchAllResources("Condition", List.of("id", "code", "subject"));
      Set<String> invalidIds = new HashSet<>();

      for (Resource resource : conditions) {
        Condition condition = (Condition) resource;
        boolean hasValidIcd = false;
        List<Coding> codings = condition.getCode().getCoding();

        for (Coding coding : codings) {
          String system = coding.getSystem();
          String codeValue = coding.getCode();

          if ("http://hl7.org/fhir/sid/icd-10".equals(system)
              || "http://hl7.org/fhir/sid/icd-10-cm".equals(system)) {
            if (codeValue != null && icdValidator.isValid(codeValue)) {
              hasValidIcd = true;
              break;
            }
          } else if ("http://hl7.org/fhir/sid/icd-9-cm".equals(system)) {
            if (codeValue != null && icdValidator.isValid(codeValue)) {
              hasValidIcd = true;
              break;
            }
          }
        }

        if (!codings.isEmpty() && !hasValidIcd) {
          String subjectRef = condition.getSubject().getReference();
          if (subjectRef != null && !subjectRef.isEmpty()) {
            invalidIds.add(subjectRef);
          }
        }
      }

      return ResultDTO.resultFromIdPaths(invalidIds, "Patient");
    } catch (Exception e) {
      log.error("Error processing {}: {}", getName(), e.getMessage());
      return new ResultDTO(e.getMessage());
    }
  }

  @Override
  public String getName() {
    return config.getName();
  }

  @Override
  public String getDescription() {
    return config.getDescription();
  }

  @Override
  public int getWarningThreshold() {
    return config.getWarningThreshold();
  }

  @Override
  public int getErrorThreshold() {
    return config.getErrorThreshold();
  }

  @Override
  public float getEpsilonBudget() {
    return config.getEpsilonBudget();
  }

  @Override
  public Long getId() {
    return config.getId();
  }
}
