package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.domain.Report;
import eu.bbmri_eric.quality.agent.dataquality.domain.ReportStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReportRepository extends JpaRepository<Report, Long> {
  List<Report> findAllByStatusIs(ReportStatus status);

  /**
   * Scalars-only history of published check results, newest first. Never loads entities or patient
   * ids; per-key order is deterministic ({@code generatedAt desc, id desc}).
   */
  @Query(
      """
      select new eu.bbmri_eric.quality.agent.dataquality.impl.ResultHistoryRow(
        r.checkId, r.stratum, r.rawValue, r.obfuscatedValue, r.epsilon)
      from Report rep join rep.results r
      where rep.status = :status
      order by rep.generatedAt desc, rep.id desc
      """)
  List<ResultHistoryRow> findResultHistory(@Param("status") ReportStatus status);

  /** Scalars-only history of the entity totals, newest first, deterministic as above. */
  @Query(
      """
      select new eu.bbmri_eric.quality.agent.dataquality.impl.TotalsHistoryRow(
        rep.numberOfEntities, rep.numberOfSecondaryEntities,
        rep.obfuscatedNumberOfEntities, rep.obfuscatedNumberOfSecondaryEntities)
      from Report rep
      where rep.status = :status
      order by rep.generatedAt desc, rep.id desc
      """)
  List<TotalsHistoryRow> findTotalsHistory(@Param("status") ReportStatus status);
}
