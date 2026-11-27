package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.FHIRServer;
import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;

class DuplicateIdentifierCheckTest {

  private static final String SYSTEM = "https://fhir.bbmri.de/id/patient";

  private final FHIRServer fhirStore = mock(FHIRServer.class);
  private final DuplicateIdentifierCheck check =
      new DuplicateIdentifierCheck(new QualityCheck("Duplicates", "duplicate identifiers", ""));

  private static Patient patientWithIdentifiers(String id, String system, String... values) {
    Patient patient = new Patient();
    patient.setId(id);
    for (String value : values) {
      patient.addIdentifier().setSystem(system).setValue(value);
    }
    return patient;
  }

  private ResultDTO runOn(Patient... patients) {
    List<Resource> resources = Arrays.asList(patients);
    when(fhirStore.fetchAllResources("Patient", List.of("id", "identifier"))).thenReturn(resources);
    return check.execute(fhirStore);
  }

  @Test
  void execute_withCollidingFirstIdentifiers_flagsAllHolders() {
    ResultDTO result =
        runOn(
            patientWithIdentifiers("p1", SYSTEM, "v"),
            patientWithIdentifiers("p2", SYSTEM, "v"),
            patientWithIdentifiers("p3", SYSTEM, "v"));

    assertEquals(3, result.rawResult());
    assertEquals(Set.of("p1", "p2", "p3"), result.idSet());
  }

  @Test
  void execute_addingPatientWithCollidingFirstIdentifier_changesCountByAtMostTwo() {
    // Baseline: one holder of v is not a duplicate.
    assertEquals(0, runOn(patientWithIdentifiers("p1", SYSTEM, "v")).rawResult());

    // The new patient flags itself and the previously unflagged holder: delta = 2.
    ResultDTO twoHolders =
        runOn(patientWithIdentifiers("p1", SYSTEM, "v"), patientWithIdentifiers("p2", SYSTEM, "v"));
    assertEquals(2, twoHolders.rawResult());

    // Both other holders were already flagged: delta = 1.
    assertEquals(
        3,
        runOn(
                patientWithIdentifiers("p1", SYSTEM, "v"),
                patientWithIdentifiers("p2", SYSTEM, "v"),
                patientWithIdentifiers("p3", SYSTEM, "v"))
            .rawResult());
  }

  @Test
  void execute_addingPatientCollidingWithSeveralPatients_changesCountByAtMostTwo() {
    // Baseline: two patients holding different values each, no duplicates.
    assertEquals(
        0,
        runOn(
                patientWithIdentifiers("p1", SYSTEM, "v1"),
                patientWithIdentifiers("p2", SYSTEM, "v2"))
            .rawResult());

    // The new patient collides through both of its identifiers, but counts only through its first
    // one: it flags itself and p1 (delta 2). Counting every identifier would flag p2 as well
    // (delta 3).
    ResultDTO result =
        runOn(
            patientWithIdentifiers("p1", SYSTEM, "v1"),
            patientWithIdentifiers("p2", SYSTEM, "v2"),
            patientWithIdentifiers("p3", SYSTEM, "v1", "v2"));

    assertEquals(2, result.rawResult());
    assertEquals(Set.of("p1", "p3"), result.idSet());
  }

  @Test
  void execute_withSecondIdentifierInSameSystem_ignoresExtraIdentifiers() {
    // p1 counts through its first identifier v1; the colliding v2 is ignored, so no duplicate.
    ResultDTO result =
        runOn(
            patientWithIdentifiers("p1", SYSTEM, "v1", "v2"),
            patientWithIdentifiers("p2", SYSTEM, "v2"));

    assertEquals(0, result.rawResult());
    assertTrue(result.idSet().isEmpty());
  }

  @Test
  void execute_withIdentifierOfOtherSystem_ignoresIt() {
    ResultDTO result =
        runOn(
            patientWithIdentifiers("p1", "http://other-system", "v"),
            patientWithIdentifiers("p2", "http://other-system", "v"));

    assertEquals(0, result.rawResult());
  }

  @Test
  void execute_withEmptyFirstIdentifierValue_patientNotCounted() {
    // p1's first identifier in the system has no value, so p1 contributes nothing; p2 then holds
    // v alone and is not a duplicate.
    ResultDTO result =
        runOn(
            patientWithIdentifiers("p1", SYSTEM, "", "v"),
            patientWithIdentifiers("p2", SYSTEM, "v"));

    assertEquals(0, result.rawResult());
  }

  @Test
  void execute_withNonFhirStore_returnsErrorResult() {
    ResultDTO result = check.execute(mock(DataStore.class));

    assertNull(result.rawResult());
    assertEquals("FHIR data store required for Duplicates", result.error());
  }

  @Test
  void getSensitivity_returnsTwo() {
    assertEquals(2, check.getSensitivity());
  }
}
