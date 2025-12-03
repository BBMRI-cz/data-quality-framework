package eu.bbmri_eric.quality.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Centralized logger for all application event.
 *
 * <p>This component asynchronously logs all application event to provide comprehensive audit trails
 * and debugging information. Events are logged with their simple class name.
 *
 * <p>Logging is done asynchronously to avoid blocking the main application flow.
 */
@Component
class ApplicationEventLogger {

  private static final Logger log = LoggerFactory.getLogger(ApplicationEventLogger.class);

  /**
   * Logs any application event.
   *
   * @param event the ApplicationEvent
   */
  @EventListener
  @Async
  void logApplicationEvent(ApplicationEvent event) {
    log.debug("EVENT: {}", event.getClass().getSimpleName());
  }
}
