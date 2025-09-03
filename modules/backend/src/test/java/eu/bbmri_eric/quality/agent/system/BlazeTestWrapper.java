package eu.bbmri_eric.quality.agent.system;

import eu.bbmri_eric.quality.agent.common.ApplicationProperties;
import eu.bbmri_eric.quality.agent.fhir.Blaze;
import org.springframework.boot.web.client.RestTemplateBuilder;

public class BlazeTestWrapper extends Blaze {
  public BlazeTestWrapper(ApplicationProperties applicationProperties, int mappedPort) {
    super(applicationProperties, new RestTemplateBuilder());

    // Add interceptor to fix URLs in paginated blaze responses
    getClient().registerInterceptor(new PagingUrlInterceptor(mappedPort));
  }
}
