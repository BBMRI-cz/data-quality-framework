package eu.bbmri_eric.quality.agent.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationProperties {
  @Value("${eu.bbmri_eric.quality.agent.fhir_url}")
  private String fhirUrl;

  @Value("${eu.bbmri_eric.quality.agent.fhir_username}")
  private String fhirUsername;

  @Value("${eu.bbmri_eric.quality.agent.fhir_password}")
  private String fhirPassword;

  public String getBaseFHIRUrl() {
    return fhirUrl != null ? fhirUrl.trim() : null;
  }

  public String getFhirUsername() {
    return fhirUsername;
  }

  public String getFhirPassword() {
    return fhirPassword;
  }
}
