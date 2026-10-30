package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class StaleReportReaperTest {

  private static final Instant NOW = Instant.parse("2026-10-30T12:00:00Z");

  private final ReportRepository reportRepository = mock(ReportRepository.class);
  private final Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);

  private StaleReportReaper reaper(Duration staleTimeout) {
    return new StaleReportReaper(reportRepository, fixedClock, staleTimeout);
  }

  private Report generatingAt(LocalDateTime generatedAt) {
    Report report = new Report();
    report.setStatus(ReportStatus.GENERATING);
    report.setGeneratedAt(generatedAt);
    return report;
  }

  @Test
  void failStaleAndGetActiveGeneratingReports_failsOnlyReportsOlderThanTheTimeout() {
    Report stale = generatingAt(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).minusHours(7));
    Report fresh = generatingAt(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).minusHours(1));
    Report undated = generatingAt(null);
    when(reportRepository.findAllByStatusIs(ReportStatus.GENERATING))
        .thenReturn(List.of(stale, fresh, undated));

    List<Report> active = reaper(Duration.ofHours(6)).failStaleAndGetActiveGeneratingReports();

    assertThat(stale.getStatus()).isEqualTo(ReportStatus.FAILED);
    assertThat(active).containsExactlyInAnyOrder(fresh, undated);
    verify(reportRepository).saveAll(List.of(stale));
  }

  @Test
  void failStaleAndGetActiveGeneratingReports_withLongerTimeout_keepsFreshByConfiguration() {
    Report sevenHoursOld = generatingAt(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).minusHours(7));
    when(reportRepository.findAllByStatusIs(ReportStatus.GENERATING))
        .thenReturn(List.of(sevenHoursOld));

    List<Report> active = reaper(Duration.ofDays(1)).failStaleAndGetActiveGeneratingReports();

    assertThat(sevenHoursOld.getStatus()).isEqualTo(ReportStatus.GENERATING);
    assertThat(active).containsExactly(sevenHoursOld);
    verify(reportRepository, never()).saveAll(anyList());
  }

  @Test
  void failStaleAndGetActiveGeneratingReports_withNoGeneratingReports_doesNothing() {
    when(reportRepository.findAllByStatusIs(ReportStatus.GENERATING)).thenReturn(List.of());

    List<Report> active = reaper(Duration.ofHours(6)).failStaleAndGetActiveGeneratingReports();

    assertThat(active).isEmpty();
    verify(reportRepository, never()).saveAll(anyList());
  }
}
