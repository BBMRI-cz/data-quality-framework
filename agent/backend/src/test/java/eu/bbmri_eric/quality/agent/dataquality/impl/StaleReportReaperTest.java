package eu.bbmri_eric.quality.agent.dataquality.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.quality.agent.dataquality.ReportProperties;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class StaleReportReaperTest {

  private static final Instant NOW = Instant.parse("2026-10-30T12:00:00Z");
  private static final java.time.LocalDateTime NOW_LDT =
      java.time.LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

  private final ReportRepository reportRepository = mock(ReportRepository.class);
  private final Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);

  private StaleReportReaper reaper(Duration staleTimeout) {
    return new StaleReportReaper(reportRepository, fixedClock, new ReportProperties(staleTimeout));
  }

  private Report generatingAt(java.time.LocalDateTime generatedAt) {
    Report report = new Report();
    report.setStatus(ReportStatus.GENERATING);
    report.setGeneratedAt(generatedAt);
    return report;
  }

  @Test
  void failStale_failsOnlyReportsOlderThanTheTimeout() {
    Report stale = generatingAt(NOW_LDT.minusHours(7));
    Report fresh = generatingAt(NOW_LDT.minusHours(1));
    Report undated = generatingAt(null);
    when(reportRepository.findAllByStatusIs(ReportStatus.GENERATING))
        .thenReturn(List.of(stale, fresh, undated));

    reaper(Duration.ofHours(6)).failStale();

    assertThat(stale.getStatus()).isEqualTo(ReportStatus.FAILED);
    verify(reportRepository).saveAll(List.of(stale));
    assertThat(reaper(Duration.ofHours(6)).activeGeneratingReports())
        .containsExactlyInAnyOrder(fresh, undated);
  }

  @Test
  void failStale_withLongerTimeout_keepsFreshByConfiguration() {
    Report sevenHoursOld = generatingAt(NOW_LDT.minusHours(7));
    when(reportRepository.findAllByStatusIs(ReportStatus.GENERATING))
        .thenReturn(List.of(sevenHoursOld));

    reaper(Duration.ofDays(1)).failStale();

    assertThat(sevenHoursOld.getStatus()).isEqualTo(ReportStatus.GENERATING);
    verify(reportRepository, never()).saveAll(anyList());
  }

  @Test
  void failStale_withNoGeneratingReports_doesNothing() {
    when(reportRepository.findAllByStatusIs(ReportStatus.GENERATING)).thenReturn(List.of());

    reaper(Duration.ofHours(6)).failStale();

    assertThat(reaper(Duration.ofHours(6)).activeGeneratingReports()).isEmpty();
    verify(reportRepository, never()).saveAll(anyList());
  }
}
