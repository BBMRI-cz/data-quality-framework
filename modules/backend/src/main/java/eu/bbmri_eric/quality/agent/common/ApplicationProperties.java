package eu.bbmri_eric.quality.agent.common;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationProperties {
    @Value("${eu.bbmri_eric.quality.agent.fhir_url}")
    private String fhirUrl;

    public String getBaseFHIRUrl() {
        return fhirUrl;
    }
}

