package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DifferentialPrivacyUtil;
import eu.bbmri_eric.quality.agent.dataquality.ReportPipelineStep;
import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class ObfuscationStep implements ReportPipelineStep {

  @Override
  public Report execute(Report report) {
    log.info("Adding obfuscated values for report id: {}", report.getId());
    for (Result result : report.getResults()) {
      if (result.getRawValue() != null) {
        double noisyValue =
            DifferentialPrivacyUtil.addLaplaceNoise(result.getRawValue(), result.getEpsilon(), 1);
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
