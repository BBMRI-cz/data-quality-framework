package eu.bbmri_eric.quality.agent.config;

import java.util.stream.Stream;
import org.springframework.modulith.core.ApplicationModuleDetectionStrategy;
import org.springframework.modulith.core.ApplicationModuleInformation;
import org.springframework.modulith.core.JavaPackage;
import org.springframework.modulith.core.NamedInterfaces;

public class CustomApplicationModuleDetectionStrategy
    implements ApplicationModuleDetectionStrategy {
  @Override
  public Stream<JavaPackage> getModuleBasePackages(JavaPackage rootPackage) {
    return rootPackage.getDirectSubPackages().stream();
  }

  @Override
  public NamedInterfaces detectNamedInterfaces(
      JavaPackage basePackage, ApplicationModuleInformation information) {
    return NamedInterfaces.builder(basePackage)
        .recursive()
        .matching("dto", "event", "exception")
        .build();
  }
}
