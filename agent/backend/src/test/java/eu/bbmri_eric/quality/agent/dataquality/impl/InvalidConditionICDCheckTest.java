package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.FHIRServer;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;

class InvalidConditionICDCheckTest {

  private static final String ICD10_SYSTEM = "http://hl7.org/fhir/sid/icd-10";

  private final FHIRServer fhirStore = mock(FHIRServer.class);
  private final InvalidConditionICDCheck check =
      new InvalidConditionICDCheck(new QualityCheck("Invalid ICD-10", "invalid ICD-10 codes", ""));

  private static Condition conditionWithCodes(String id, String subjectRef, String... codes) {
    Condition condition = new Condition();
    condition.setId(id);
    condition.getSubject().setReference(subjectRef);
    for (String code : codes) {
      condition.getCode().addCoding().setSystem(ICD10_SYSTEM).setCode(code);
    }
    return condition;
  }

  private ResultDTO runOn(Condition... conditions) {
    List<Resource> resources = new ArrayList<>(Arrays.asList(conditions));
    when(fhirStore.fetchAllResources("Condition", List.of("id", "code", "subject")))
        .thenReturn(resources);
    return check.execute(fhirStore);
  }

  @Test
  void execute_withValidIcdCoding_patientNotCounted() {
    ResultDTO result =
        runOn(
            conditionWithCodes("c1", "Patient/p1", "E11"),
            conditionWithCodes("c2", "Patient/p2", "ZZZ"));

    assertEquals(1, result.rawResult());
    assertEquals(List.of("p2"), List.copyOf(result.idSet()));
  }

  @Test
  void execute_addingPatientWithManyInvalidConditions_increasesCountByAtMostOne() {
    // Baseline: one patient with one invalid condition.
    assertEquals(1, runOn(conditionWithCodes("c1", "Patient/p1", "ZZZ")).rawResult());

    // The new patient brings fifty invalid conditions but is a single subject: the count of
    // distinct subjects rises by exactly 1.
    List<Condition> conditions = new ArrayList<>();
    conditions.add(conditionWithCodes("c1", "Patient/p1", "ZZZ"));
    for (int i = 0; i < 50; i++) {
      conditions.add(conditionWithCodes("p2-c" + i, "Patient/p2", "ZZZ"));
    }
    ResultDTO result = runOn(conditions.toArray(Condition[]::new));

    assertEquals(2, result.rawResult());
  }

  @Test
  void getSensitivity_returnsOne() {
    assertEquals(1, check.getSensitivity());
  }
}
