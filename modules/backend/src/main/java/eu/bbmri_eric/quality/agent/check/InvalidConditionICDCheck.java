package eu.bbmri_eric.quality.agent.check;

import eu.bbmri_eric.ICD10Validator;
import eu.bbmri_eric.ICDValidator;
import eu.bbmri_eric.quality.agent.fhir.FHIRStore;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Resource;

public class InvalidConditionICDCheck implements Check {
  private final String name;
  private final String description;

  public InvalidConditionICDCheck() {
    this.name = "Invalid ICD-10 Codes";
    this.description = "How many conditions have invalid ICD-10 codes";
  }

  @Override
  public Result execute(FHIRStore fhirStore) {
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

      int count = invalidIds.size();
      return new Result(count, "Patient", null);
    } catch (Exception e) {
      System.err.println("Error processing " + name + ": " + e.getMessage());
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
    return 30;
  }

  @Override
  public float getEpsilonBudget() {
    return 0.2f;
  }

  @Override
  public Long getId() {
    return 1001L;
  }
}
