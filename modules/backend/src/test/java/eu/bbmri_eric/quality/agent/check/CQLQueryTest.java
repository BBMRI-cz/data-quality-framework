package eu.bbmri_eric.quality.agent.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.fhir.FHIRStore;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class CQLQueryTest {

  private void stubMeasureSetup(FHIRStore store) throws Exception {
    when(store.createLibrary(anyString(), anyString())).thenReturn(new JSONObject());
    when(store.postResource(eq("Library"), any(JSONObject.class))).thenReturn(new JSONObject());
    when(store.createMeasure(anyString(), anyString(), anyString()))
        .thenReturn(new JSONObject().put("id", "measure1"));
    when(store.postResource(eq("Measure"), any(JSONObject.class)))
        .thenReturn(new JSONObject().put("id", "measure1"));
  }

  private JSONObject measureReportWith(int count, String listRefOrNull) throws Exception {
    JSONObject pop =
        new JSONObject()
            .put(
                "code",
                new JSONObject()
                    .put(
                        "coding",
                        new JSONArray()
                            .put(
                                new JSONObject()
                                    .put(
                                        "system",
                                        "http://terminology.hl7.org/CodeSystem/measure-population")
                                    .put("code", "initial-population"))))
            .put("count", count);
    if (listRefOrNull != null) {
      pop.put("subjectResults", new JSONObject().put("reference", listRefOrNull));
    }
    return new JSONObject()
        .put(
            "group",
            new JSONArray().put(new JSONObject().put("population", new JSONArray().put(pop))));
  }

  private JSONObject patientList(Object... refs) throws Exception {
    JSONArray entries = new JSONArray();
    for (Object ref : refs) {
      if (ref == null) {
        entries.put(new JSONObject()); // missing item/reference
      } else if (ref instanceof String s) {
        entries.put(new JSONObject().put("item", new JSONObject().put("reference", s)));
      } else if (ref instanceof JSONObject jo) {
        entries.put(jo);
      }
    }
    return new JSONObject().put("entry", entries);
  }

  @Test
  void execute_returnsCorrectPatientIds() throws Exception {
    FHIRStore mockStore = mock(FHIRStore.class);
    CQLQuery query = new CQLQuery("name", "desc", "query");

    stubMeasureSetup(mockStore);
    // measureReport
    when(mockStore.evaluateMeasureList("measure1"))
        .thenReturn(measureReportWith(2, "List/list123"));

    // Patient list
    JSONObject listResource =
        new JSONObject()
            .put(
                "entry",
                new JSONArray()
                    .put(
                        new JSONObject()
                            .put("item", new JSONObject().put("reference", "Patient/p1")))
                    .put(
                        new JSONObject()
                            .put("item", new JSONObject().put("reference", "Patient/p2"))));

    when(mockStore.getPatientList("list123")).thenReturn(patientList("Patient/p1", "Patient/p2"));

    // Act
    Result result = query.execute(mockStore);

    // Assert
    assertEquals(Set.of("p1", "p2"), result.idSet());
    assertEquals(2, result.numberOfEntities());
  }

  @Test
  void execute_returnsEmptySet_whenPopulationCountIsZero_andDoesNotCallGetPatientList()
      throws Exception {
    FHIRStore store = mock(FHIRStore.class);
    CQLQuery query = new CQLQuery("name", "desc", "query");
    stubMeasureSetup(store);

    when(store.evaluateMeasureList("measure1")).thenReturn(measureReportWith(0, "List/list123"));

    Result result = query.execute(store);

    assertEquals(Set.of(), result.idSet());
    assertEquals(0, result.numberOfEntities());
    verify(store, never()).getPatientList(anyString());
  }

  @Test
  void execute_returnsEmptySet_whenListReferenceMissing_evenIfCountPositive() throws Exception {
    FHIRStore store = mock(FHIRStore.class);
    CQLQuery query = new CQLQuery("name", "desc", "query");
    stubMeasureSetup(store);

    when(store.evaluateMeasureList("measure1")).thenReturn(measureReportWith(3, null));

    Result result = query.execute(store);

    assertEquals(Set.of(), result.idSet());
    assertEquals(3, result.numberOfEntities());
    verify(store, never()).getPatientList(anyString());
  }
}
