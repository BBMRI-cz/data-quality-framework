package eu.bbmri_eric.quality.agent.dataquality;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Report-generation configuration. {@code staleGeneratingTimeout} is how long a report may stay in
 * {@code GENERATING} before the stale-report reaper fails it (default 6 hours).
 */
@ConfigurationProperties(prefix = "app.report")
public record ReportProperties(Duration staleGeneratingTimeout) {

  static final Duration DEFAULT_STALE_TIMEOUT = Duration.ofHours(6);

  public ReportProperties {
    if (staleGeneratingTimeout == null) {
      staleGeneratingTimeout = DEFAULT_STALE_TIMEOUT;
    }
  }
}
