package eu.bbmri_eric.quality.agent.system;

import eu.bbmri_eric.quality.agent.common.ApplicationProperties;
import eu.bbmri_eric.quality.agent.fhir.Blaze;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class BlazeTestConfig {
  @Bean
  @Primary
  public Blaze blaze(ApplicationProperties applicationProperties) {
    int mappedPort = QualityCheckSystemTest.blazeContainer.getMappedPort(8080);
    return new BlazeTestWrapper(applicationProperties, mappedPort);
  }
}
