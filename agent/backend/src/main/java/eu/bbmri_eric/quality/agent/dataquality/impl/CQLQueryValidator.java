package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.domain.CQLQuery;
import jakarta.validation.Valid;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
class CQLQueryValidator {

  @HandleBeforeCreate
  public void validateBeforeCreate(@Valid CQLQuery query) {
    // validation is triggered automatically
  }

  @HandleBeforeSave
  public void validateBeforeSave(@Valid CQLQuery query) {
    // validation is triggered automatically
  }
}
