package eu.bbmri_eric.quality.agent.dataquality.privacy;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wiring for the differential-privacy primitives: one randomness source per application. */
@Configuration
class PrivacyConfiguration {

  /** Cryptographically strong randomness for all privacy mechanisms; inject where needed. */
  @Bean
  RandomGenerator randomGenerator() {
    return new SecureRandom();
  }
}
