package eu.bbmri_eric.quality.agent.config;

import eu.bbmri_eric.quality.agent.dataquality.ReportProperties;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReportProperties.class)
class ClockConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemDefaultZone();
  }
}
