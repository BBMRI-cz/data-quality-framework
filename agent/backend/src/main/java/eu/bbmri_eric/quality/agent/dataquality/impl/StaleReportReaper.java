package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.ReportProperties;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the stale-report policy: only one report may be generated at a time, so a generation that
 * crashes or hangs must be failed automatically to unblock future reports. Stale reports are failed
 * on an hourly sweep, on every report creation (eagerly, so a new report never waits for the next
 * sweep) and unconditionally at startup (generation is in-process, so after a restart no report can
 * still be running).
 */
@Slf4j
@Component
class StaleReportReaper {

  private final ReportRepository reportRepository;
  private final Clock clock;
  private final Duration staleTimeout;

  StaleReportReaper(ReportRepository reportRepository, Clock clock, ReportProperties properties) {
    this.reportRepository = reportRepository;
    this.clock = clock;
    this.staleTimeout = properties.staleGeneratingTimeout();
  }

  /** Hourly sweep for generations that hang or die while the application keeps running. */
  @Scheduled(cron = "0 0 * * * *")
  @Transactional
  public void failStaleGeneratingReports() {
    failStale();
  }

  /** After a restart no generation can still be running, so all of them are failed. */
  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void failStaleGeneratingReportsOnStartup() {
    List<Report> stuck = reportRepository.findAllByStatusIs(ReportStatus.GENERATING);
    if (stuck.isEmpty()) {
      return;
    }
    log.warn("Marking {} report(s) stuck in GENERATING as FAILED after restart", stuck.size());
    markAllFailed(stuck);
  }

  /**
   * Fails reports stuck in {@link ReportStatus#GENERATING} for longer than the staleness timeout. A
   * report without a timestamp is not auto-failed, since its age cannot be determined.
   */
  @Transactional
  public void failStale() {
    LocalDateTime staleBefore = LocalDateTime.now(clock).minus(staleTimeout);
    List<Report> stale =
        reportRepository.findAllByStatusIs(ReportStatus.GENERATING).stream()
            .filter(report -> isStale(report, staleBefore))
            .toList();
    if (!stale.isEmpty()) {
      log.warn("Marking {} stale report(s) stuck in GENERATING as FAILED", stale.size());
      markAllFailed(stale);
    }
  }

  /**
   * @return reports still actively generating (after any prior call that failed stale ones).
   */
  @Transactional(readOnly = true)
  public List<Report> activeGeneratingReports() {
    LocalDateTime staleBefore = LocalDateTime.now(clock).minus(staleTimeout);
    return reportRepository.findAllByStatusIs(ReportStatus.GENERATING).stream()
        .filter(report -> !isStale(report, staleBefore))
        .toList();
  }

  private void markAllFailed(List<Report> reports) {
    reports.forEach(Report::markFailed);
    reportRepository.saveAll(reports);
  }

  private static boolean isStale(Report report, LocalDateTime staleBefore) {
    return report.getGeneratedAt() != null && report.getGeneratedAt().isBefore(staleBefore);
  }
}
