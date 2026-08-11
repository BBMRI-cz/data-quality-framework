package eu.bbmri_eric.quality.agent.logs.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the in-memory log cache and attaches the capturing appender to the root logger. */
@Configuration
class LoggingConfiguration {

  static final String APPENDER_NAME = "LOG_CAPTURE";

  private final int maxLogEntries;

  LoggingConfiguration(@Value("${logging.in-memory.max-entries:500}") int maxLogEntries) {
    this.maxLogEntries = maxLogEntries;
  }

  @Bean
  LogCache logCache() {
    return new LogCache(maxLogEntries);
  }

  @Bean
  LogbackCapturingAppender logbackCapturingAppender(LogCache logCache) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    LogbackCapturingAppender appender = new LogbackCapturingAppender(logCache);
    appender.setName(APPENDER_NAME);
    appender.setContext(context);
    appender.start();
    if (rootLogger.getAppender(APPENDER_NAME) == null) {
      rootLogger.addAppender(appender);
    }
    return appender;
  }
}
