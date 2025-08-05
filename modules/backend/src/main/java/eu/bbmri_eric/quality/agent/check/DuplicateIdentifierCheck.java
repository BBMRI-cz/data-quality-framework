package eu.bbmri_eric.quality.agent.check;

import eu.bbmri_eric.quality.agent.fhir.FHIRStore;
import java.util.*;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DuplicateIdentifierCheck implements DataQualityCheck {
  private static final Logger log = LoggerFactory.getLogger(DuplicateIdentifierCheck.class);
  private final String name;
  private final String description;
  private final String identifierSystem;

  DuplicateIdentifierCheck() {
    this("https://fhir.bbmri.de/id/patient");
  }

  DuplicateIdentifierCheck(String identifierSystem) {
    this.name = "Duplicate identifiers";
    this.description = "Duplicate patients";
    this.identifierSystem = identifierSystem;
  }

  @Override
  public Result execute(FHIRStore fhirStore) {
    try {
      List<Resource> patients = fhirStore.fetchAllResources("Patient", List.of("id", "identifier"));
      Map<String, List<String>> identifierMap = new HashMap<>();
      for (Resource resource : patients) {
        Patient patient = (Patient) resource;
        String patientId = patient.getIdElement().getIdPart();
        List<Identifier> identifiers = patient.getIdentifier();
        for (Identifier ident : identifiers) {
          log.info(ident.toString());
          if (getIdentifierSystem().equals(ident.getSystem())) {
            String identValue = ident.getValue();
            log.info(identValue);
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
      int count = duplicateIds.size();
      List<String> patientIds =
          duplicateIds.stream()
              .map(
                  path -> {
                    String[] split = path.split("/");
                    return split[split.length - 1];
                  })
              .toList();
      return new Result(count, "Patient", patientIds);
    } catch (Exception e) {
      log.error("Error processing {}: {}", getName(), e.getMessage());
      return new Result(e.getMessage());
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public int getWarningThreshold() {
    return 10;
  }

  @Override
  public int getErrorThreshold() {
    return 20;
  }

  @Override
  public float getEpsilonBudget() {
    return 0.2f;
  }

  @Override
  public Long getId() {
    return 1000L;
  }

  private String getIdentifierSystem() {
    return identifierSystem;
  }
}
