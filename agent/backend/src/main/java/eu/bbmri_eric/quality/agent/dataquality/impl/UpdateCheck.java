package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.FHIRStore;
import eu.bbmri_eric.quality.agent.dataquality.domain.DataQualityCheck;
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

public class UpdateCheck implements DataQualityCheck {
  private static final Logger log = LoggerFactory.getLogger(UpdateCheck.class);
  private static final String NAME = "Stale patient updates";
  private static final String DESCRIPTION = "Patients last updated more than one month ago";
  private final Clock clock;

  UpdateCheck() {
    this(Clock.systemUTC());
  }

  UpdateCheck(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public ResultDTO execute(FHIRStore fhirStore) {
    try {
      List<Resource> patients = fhirStore.fetchAllResources("Patient", List.of("id", "meta"));
      Instant cutoff = ZonedDateTime.now(clock).minusMonths(1).toInstant();
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
    return NAME;
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public int getWarningThreshold() {
    return 10;
  }

  @Override
  public int getErrorThreshold() {
    return 30;
  }

  @Override
  public float getEpsilonBudget() {
    return 0.2f;
  }

  @Override
  public Long getId() {
    return 1004L;
  }
}
