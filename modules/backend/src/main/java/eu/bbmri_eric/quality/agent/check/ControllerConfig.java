package eu.bbmri_eric.quality.agent.check;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
class ControllerConfig implements RepositoryRestConfigurer {

  Validator validator;

  public ControllerConfig(Validator validator) {
    this.validator = validator;
  }

  @Override
  public void configureRepositoryRestConfiguration(
      RepositoryRestConfiguration config, CorsRegistry cors) {
    config.exposeIdsFor(CQLQuery.class);
  }

  @Override
  public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener validatingListener) {
    RepositoryRestConfigurer.super.configureValidatingRepositoryEventListener(validatingListener);
    validatingListener.addValidator("beforeCreate", validator);
    validatingListener.addValidator("beforeSave", validator);
  }
}
