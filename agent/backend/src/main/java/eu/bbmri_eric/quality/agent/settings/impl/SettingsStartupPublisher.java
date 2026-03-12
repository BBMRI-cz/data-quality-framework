package eu.bbmri_eric.quality.agent.settings.impl;

import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import eu.bbmri_eric.quality.agent.settings.event.SettingsUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SettingsStartupPublisher {

  private static final Logger log = LoggerFactory.getLogger(SettingsStartupPublisher.class);

  private final SettingsService settingsService;
  private final EventPublisher eventPublisher;

  public SettingsStartupPublisher(SettingsService settingsService, EventPublisher eventPublisher) {
    this.settingsService = settingsService;
    this.eventPublisher = eventPublisher;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void publishSettingsOnStartup() {
    log.info("Application ready, publishing initial settings event");
    SettingsDTO settings = settingsService.getSettings();
    eventPublisher.publishEvent(new SettingsUpdatedEvent(settings));
  }
}
