package eu.bbmri_eric.quality.agent.logs.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the in-memory log cache and attaches the capturing appender to the root logger. All Spring
 * contexts share the same {@link LogCache} instance so that any context's log service reads the
 * entries captured by the (single, global) root appender.
 */
@Configuration
class LoggingConfiguration {

  static final String APPENDER_NAME = "LOG_CAPTURE";

  @Bean
  LogCache logCache() {
    return LogCache.getShared();
  }

  @Bean
  LogbackCapturingAppender logbackCapturingAppender(LogCache logCache) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    Appender<ILoggingEvent> existing = rootLogger.getAppender(APPENDER_NAME);
    if (existing != null) {
      existing.stop();
      rootLogger.detachAppender(existing);
    }
    LogbackCapturingAppender appender = new LogbackCapturingAppender(logCache);
    appender.setName(APPENDER_NAME);
    appender.setContext(context);
    appender.start();
    rootLogger.addAppender(appender);
    return appender;
  }
}
