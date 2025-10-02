package eu.bbmri_eric.quality.server.common;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for ModelMapper bean.
 * Provides the ModelMapper instance required by the application.
 */
@Configuration
class ModelMapperConfig {

  /**
   * Creates a ModelMapper bean for object mapping.
   *
   * @return configured ModelMapper instance
   */
  @Bean
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }
}
