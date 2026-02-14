package eu.bbmri_eric.quality.agent.dataquality;

import eu.bbmri_eric.quality.agent.common.CRUDService;
import eu.bbmri_eric.quality.agent.dataquality.dto.ObfuscatedReportDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportUpdateDTO;

public interface ReportService
    extends CRUDService<ReportDTO, ReportCreateDTO, ReportUpdateDTO, Long> {

  /**
   * Get a report by ID.
   *
   * @param id the report ID
   * @return the report as a DTO with obfuscated values safe for sharing
   */
  ObfuscatedReportDTO getObfuscatedById(Long id);
}
