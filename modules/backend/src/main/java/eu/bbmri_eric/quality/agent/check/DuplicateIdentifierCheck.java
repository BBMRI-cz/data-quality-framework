package eu.bbmri_eric.quality.agent.check;

import eu.bbmri_eric.quality.agent.fhir.FHIRStore;
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

public class DuplicateIdentifierCheck implements DataQualityCheck {
  private static final Logger log = LoggerFactory.getLogger(DuplicateIdentifierCheck.class);
  private final String name;
  private final String description;
  private final String identifierSystem;

  public DuplicateIdentifierCheck() {
    this("https://fhir.bbmri.de/id/patient");
  }

  public DuplicateIdentifierCheck(String identifierSystem) {
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
          if (identifierSystem.equals(ident.getSystem())) {
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
      return new Result(count, "Patient");
    } catch (Exception e) {
      log.error("Error processing {}: {}", name, e.getMessage());
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
}
