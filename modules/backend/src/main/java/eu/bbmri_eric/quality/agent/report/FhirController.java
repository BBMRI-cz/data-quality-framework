package eu.bbmri_eric.quality.agent.report;

import eu.bbmri_eric.quality.agent.fhir.FHIRStore;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fhir")
public class FhirController {
  private final FHIRStore fhirStore;

  public FhirController(FHIRStore fhirStore) {
    this.fhirStore = fhirStore;
  }

  @GetMapping("/Patient/{id}/$everything")
  public ResponseEntity<String> getPatientEverything(@PathVariable String id) {
    try {
      JSONObject patientEverything = fhirStore.getPatientEverything(id);
      return new ResponseEntity<>(patientEverything.toString(), HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(
          "Failed to retrieve patient everything:" + e, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
