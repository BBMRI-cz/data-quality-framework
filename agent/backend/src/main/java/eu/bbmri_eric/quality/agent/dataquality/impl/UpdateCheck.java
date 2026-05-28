package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.FHIRServer;
import eu.bbmri_eric.quality.agent.dataquality.domain.DataQualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UpdateCheck implements DataQualityCheck {
  private static final Logger log = LoggerFactory.getLogger(UpdateCheck.class);
  static final Long CHECK_ID = 1003L;

  private final QualityCheck config;
  private final Clock clock;

  UpdateCheck(QualityCheck config) {
    this(config, Clock.systemUTC());
  }

  UpdateCheck(QualityCheck config, Clock clock) {
    this.config = config;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public ResultDTO execute(DataStore dataStore) {
    if (!(dataStore instanceof FHIRServer fhirStore)) {
      return new ResultDTO("FHIR data store required for " + getName());
    }
    try {
      List<Resource> patients = fhirStore.fetchAllResources("Patient", List.of("id", "meta"));
      Instant cutoff = ZonedDateTime.now(clock).minusMonths(3).toInstant();
      Set<String> staleIds = new HashSet<>();
      for (Resource resource : patients) {
        if (resource instanceof Patient patient) {
          Date lastUpdated = patient.getMeta().getLastUpdated();
          if (lastUpdated == null) {
            continue;
          }
          if (lastUpdated.toInstant().isBefore(cutoff)) {
            String patientId = patient.getIdElement().getIdPart();
            if (patientId != null && !patientId.isBlank()) {
              staleIds.add("Patient/" + patientId);
            }
          }
        }
      }
      return ResultDTO.resultFromIdPaths(staleIds, "Patient");
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
  public Double getEpsilonBudget() {
    return config.getEpsilonBudget();
  }

  @Override
  public Long getId() {
    return config.getId();
  }
}
