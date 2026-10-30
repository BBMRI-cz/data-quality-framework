package eu.bbmri_eric.quality.agent.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClockConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
