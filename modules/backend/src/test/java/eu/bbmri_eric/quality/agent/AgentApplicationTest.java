package eu.bbmri_eric.quality.agent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;

@SpringBootTest
class AgentApplicationTest {

  @Test
  void contextLoads() {
    ApplicationModules.of(AgentApplication.class).verify();
  }
}
