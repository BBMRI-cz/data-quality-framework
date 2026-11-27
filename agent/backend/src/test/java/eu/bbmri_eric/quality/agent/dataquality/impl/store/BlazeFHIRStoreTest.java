package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Specimen;
import org.junit.jupiter.api.Test;

class BlazeFHIRStoreTest {

  private static Specimen specimen(String id, String subjectReference) {
    Specimen specimen = new Specimen();
    specimen.setId(id);
    if (subjectReference != null) {
      specimen.getSubject().setReference(subjectReference);
    }
    return specimen;
  }

  private static List<Resource> specimensFor(String subjectReference, int count) {
    List<Resource> specimens = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      specimens.add(specimen(subjectReference + "-s" + i, subjectReference));
    }
    return specimens;
  }

  @Test
  void cappedSampleTotal_capsEachSubjectAtTwenty() {
    List<Resource> specimens = new ArrayList<>(specimensFor("Patient/1", 50));
    specimens.addAll(specimensFor("Patient/2", 3));

    assertEquals(23, BlazeFHIRStore.cappedSampleTotal(specimens));
  }

  @Test
  void cappedSampleTotal_addingSubjectWithHundredSpecimens_raisesTotalByTwenty() {
    List<Resource> specimens = new ArrayList<>(specimensFor("Patient/1", 3));
    int baseline = BlazeFHIRStore.cappedSampleTotal(specimens);
    specimens.addAll(specimensFor("Patient/2", 100));

    assertEquals(20, BlazeFHIRStore.cappedSampleTotal(specimens) - baseline);
  }

  @Test
  void cappedSampleTotal_ignoresSpecimensWithoutSubject() {
    List<Resource> specimens = new ArrayList<>(specimensFor("Patient/1", 2));
    specimens.add(specimen("orphan1", null));
    specimens.add(specimen("orphan2", ""));

    assertEquals(2, BlazeFHIRStore.cappedSampleTotal(specimens));
  }

  @Test
  void cappedSampleTotal_groupsBySubjectIdAcrossReferenceForms() {
    // One subject referenced two ways: grouped by id part it is one bucket capped at 20; split
    // across two keys it could contribute 25 + 15.
    List<Resource> specimens = new ArrayList<>(specimensFor("Patient/1", 25));
    specimens.addAll(specimensFor("https://example.org/fhir/Patient/1", 15));

    assertEquals(20, BlazeFHIRStore.cappedSampleTotal(specimens));
  }

  @Test
  void cappedSampleTotal_emptyListReturnsZero() {
    assertEquals(0, BlazeFHIRStore.cappedSampleTotal(List.of()));
  }
}
