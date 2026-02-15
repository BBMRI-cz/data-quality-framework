package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.FHIRStore;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

@Slf4j
class SurvivalRateCheck implements StratifiedDataQualityCheck {
  static final Long CHECK_ID = 1002L;

  private final QualityCheck config;
  private final List<String> genders;

  SurvivalRateCheck(QualityCheck config) {
    this.config = config;
    this.genders = Arrays.asList("male", "female");
  }

  @Override
  public ResultDTO execute(FHIRStore fhirStore) {
    try {
      List<Resource> patients =
          fhirStore.fetchAllResources("Patient", List.of("id", "gender", "deceased"));
      int totalAlive = 0;
      int totalCount = 0;

      for (Resource resource : patients) {
        Patient patient = (Patient) resource;
        boolean deceased = patient.hasDeceased() || patient.hasDeceasedDateTimeType();
        if (!deceased) {
          totalAlive++;
        }
        totalCount++;
      }

      return new ResultDTO(totalAlive, "Patient", Collections.emptySet());
    } catch (Exception e) {
      log.error("Error processing {}: {}", getName(), e.getMessage());
      return new ResultDTO(e.getMessage());
    }
  }

  @Override
  public Map<String, ResultDTO> executeWithStratification(FHIRStore fhirStore) {
    Map<String, ResultDTO> results = new HashMap<>();
    try {
      List<Resource> patients =
          fhirStore.fetchAllResources("Patient", List.of("id", "gender", "deceased"));
      // Stratified by gender
      for (String gender : genders) {
        Set<String> genderAliveIds = getGenderAlive(gender, patients);
        int genderAlive = genderAliveIds.size();

        results.put(gender, new ResultDTO(genderAlive, "Patient", genderAliveIds));
      }
      return results;
    } catch (Exception e) {
      log.error("Error processing stratified {}: {}", getName(), e.getMessage());
      results.put("error", new ResultDTO(e.getMessage()));
      return results;
    }
  }

  private static Set<String> getGenderAlive(String gender, List<Resource> patients) {
    Set<String> genderAliveIds = new HashSet<>();
    int genderCount = 0;

    for (Resource resource : patients) {
      Patient patient = (Patient) resource;
      if (gender.equalsIgnoreCase(
          patient.getGender() != null ? patient.getGender().toCode() : "")) {
        boolean deceased = patient.hasDeceased() || patient.hasDeceasedDateTimeType();
        if (!deceased) {
          genderAliveIds.add(patient.getIdElement().getIdPart());
        }
        genderCount++;
      }
    }
    return genderAliveIds;
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
