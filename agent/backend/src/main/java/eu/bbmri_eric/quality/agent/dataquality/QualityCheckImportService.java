package eu.bbmri_eric.quality.agent.dataquality;

import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckImportSummaryDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckTransferDTO;
import java.util.List;

/** Imports quality checks from a file in the portable export format. */
public interface QualityCheckImportService {

  /**
   * Imports quality checks, matching them by exact name: new names are created, existing ones are
   * updated only where a field differs, so re-importing the same file changes nothing. The whole
   * file is validated before anything is saved: an invalid entry rejects the import without writing
   * anything. No query is executed, no report is started and no differential privacy budget is
   * spent.
   *
   * @param entries the entries of the import file
   * @return how many checks were created, updated and left unchanged
   */
  QualityCheckImportSummaryDTO importChecks(List<QualityCheckTransferDTO> entries);
}
