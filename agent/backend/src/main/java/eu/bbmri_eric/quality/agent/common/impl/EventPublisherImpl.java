package eu.bbmri_eric.quality.agent.common.impl;

import eu.bbmri_eric.quality.agent.common.EventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class EventPublisherImpl implements EventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  public EventPublisherImpl(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public void publishEvent(Object result) {
    applicationEventPublisher.publishEvent(result);
  }
}
