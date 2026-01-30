package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import jakarta.validation.Valid;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
class QualityCheckValidator {

  @HandleBeforeCreate
  public void validateBeforeCreate(@Valid QualityCheck query) {
    // validation is triggered automatically
  }

  @HandleBeforeSave
  public void validateBeforeSave(@Valid QualityCheck query) {
    // validation is triggered automatically
  }
}
