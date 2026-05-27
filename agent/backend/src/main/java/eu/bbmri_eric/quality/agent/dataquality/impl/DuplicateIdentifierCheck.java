package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.FHIRServer;
import eu.bbmri_eric.quality.agent.dataquality.domain.DataQualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DuplicateIdentifierCheck implements DataQualityCheck {
  private static final Logger log = LoggerFactory.getLogger(DuplicateIdentifierCheck.class);
  static final Long CHECK_ID = 1000L;

  private final QualityCheck config;
  private final String identifierSystem;

  DuplicateIdentifierCheck(QualityCheck config) {
    this(config, "https://fhir.bbmri.de/id/patient");
  }

  DuplicateIdentifierCheck(QualityCheck config, String identifierSystem) {
    this.config = config;
    this.identifierSystem = identifierSystem;
  }

  @Override
  public ResultDTO execute(DataStore dataStore) {
    if (!(dataStore instanceof FHIRServer fhirStore)) {
      return new ResultDTO("FHIR data store required for " + getName());
    }
    try {
      List<Resource> patients = fhirStore.fetchAllResources("Patient", List.of("id", "identifier"));
      Map<String, List<String>> identifierMap = new HashMap<>();
      for (Resource resource : patients) {
        Patient patient = (Patient) resource;
        String patientId = patient.getIdElement().getIdPart();
        List<Identifier> identifiers = patient.getIdentifier();
        for (Identifier ident : identifiers) {
          log.debug(ident.toString());
          if (getIdentifierSystem().equals(ident.getSystem())) {
            String identValue = ident.getValue();
            log.debug(identValue);
            if (identValue != null && !identValue.isEmpty()) {
              identifierMap
                  .computeIfAbsent(identValue, k -> new ArrayList<>())
                  .add("Patient/" + patientId);
            }
          }
        }
      }
      Set<String> duplicateIds = new HashSet<>();
      for (Map.Entry<String, List<String>> entry : identifierMap.entrySet()) {
        if (entry.getValue().size() > 1) {
          duplicateIds.addAll(entry.getValue());
        }
      }
      log.info("Duplicate identifiers: {}", duplicateIds);
      return ResultDTO.resultFromIdPaths(duplicateIds, "Patient");
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
  public Float getEpsilonBudget() {
    return config.getEpsilonBudget();
  }

  @Override
  public Long getId() {
    return config.getId();
  }

  private String getIdentifierSystem() {
    return identifierSystem;
  }
}
