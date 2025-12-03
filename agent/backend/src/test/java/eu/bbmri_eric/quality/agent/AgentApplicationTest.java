package eu.bbmri_eric.quality.agent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

@SpringBootTest
class AgentApplicationTest {

  ApplicationModules modules = ApplicationModules.of(AgentApplication.class);

  @Test
  void contextLoads() {
    ApplicationModules.of(AgentApplication.class).verify();
  }

  @Test
  void writeDocumentationSnippets() {
    try {
      new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
    } catch (Exception e) {
      System.out.println("Could not write individual modules as plantuml");
    }
  }
}
