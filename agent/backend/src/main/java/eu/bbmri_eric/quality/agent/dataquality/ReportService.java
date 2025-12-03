package eu.bbmri_eric.quality.agent.dataquality;

import eu.bbmri_eric.quality.agent.dataquality.dto.ReportDTO;

public interface ReportService {

  /**
   * Generate a report transactionally. This method creates a new report and triggers the report
   * generation process.
   */
  void generateReport();

  /**
   * Get a report by ID and convert it to a DTO.
   *
   * @param id the report ID
   * @return the report as a DTO
   */
  ReportDTO getById(Long id);
}
