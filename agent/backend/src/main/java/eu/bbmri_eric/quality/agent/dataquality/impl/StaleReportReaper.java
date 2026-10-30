package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

  StaleReportReaper(
      ReportRepository reportRepository,
      Clock clock,
      @Value("${app.report.stale-generating-timeout:PT6H}") Duration staleTimeout) {
    this.reportRepository = reportRepository;
    this.clock = clock;
    this.staleTimeout = staleTimeout;
  }

  /** Hourly sweep for generations that hang or die while the application keeps running. */
  @Scheduled(cron = "0 0 * * * *")
  @Transactional
  public void failStaleGeneratingReports() {
    failStaleAndGetActiveGeneratingReports();
  }

  /** After a restart no generation can still be running, so all of them are failed. */
  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void failStaleGeneratingReportsOnStartup() {
    List<Report> staleReports = reportRepository.findAllByStatusIs(ReportStatus.GENERATING);
    if (staleReports.isEmpty()) {
      return;
    }
    log.warn(
        "Marking {} report(s) stuck in GENERATING as FAILED after restart", staleReports.size());
    staleReports.forEach(report -> report.setStatus(ReportStatus.FAILED));
    reportRepository.saveAll(staleReports);
  }

  /**
   * Fails reports stuck in {@link ReportStatus#GENERATING} for longer than the staleness timeout
   * and returns the reports that are still actively generating. Reports without a timestamp are not
   * auto-failed, since their age cannot be determined.
   *
   * @return reports in {@link ReportStatus#GENERATING} that are not stale
   */
  List<Report> failStaleAndGetActiveGeneratingReports() {
    LocalDateTime staleBefore = LocalDateTime.now(clock).minus(staleTimeout);
    List<Report> generatingReports = reportRepository.findAllByStatusIs(ReportStatus.GENERATING);
    List<Report> staleReports =
        generatingReports.stream().filter(report -> isStale(report, staleBefore)).toList();
    if (!staleReports.isEmpty()) {
      log.warn("Marking {} stale report(s) stuck in GENERATING as FAILED", staleReports.size());
      staleReports.forEach(report -> report.setStatus(ReportStatus.FAILED));
      reportRepository.saveAll(staleReports);
    }
    return generatingReports.stream().filter(report -> !isStale(report, staleBefore)).toList();
  }

  private static boolean isStale(Report report, LocalDateTime staleBefore) {
    return report.getGeneratedAt() != null && report.getGeneratedAt().isBefore(staleBefore);
  }
}
