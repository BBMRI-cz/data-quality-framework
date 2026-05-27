package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DifferentialPrivacyUtil;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class ObfuscationStep implements ReportPipelineStep {
  private final SettingsService settingsService;

  ObfuscationStep(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  @Override
  public Report execute(Report report) {
    var reportEpsilonBudget = settingsService.getSettings().getEpsilon();
    var numberOfResults = report.getResults().size();
    log.info("Adding obfuscated values for report id: {}", report.getId());
    for (Result result : report.getResults()) {
      if (result.getRawValue() != null) {
        double noisyValue =
            DifferentialPrivacyUtil.addLaplaceNoise(
                result.getRawValue(), reportEpsilonBudget / numberOfResults, 1);
        result.setObfuscatedValue(noisyValue);
      }
    }
    log.info("Completed obfuscation for report id: {}", report.getId());
    return report;
  }

  @Override
  public int getOrder() {
    return 20;
  }
}
