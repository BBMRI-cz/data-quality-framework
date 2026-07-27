package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface QualityCheckRepository extends JpaRepository<QualityCheck, Long> {

  /**
   * Finds quality checks that belong to a category with the given name.
   *
   * @param categoryName the exact category name to filter by
   * @param pageable pagination and sorting information
   * @return a page of matching quality checks
   */
  Page<QualityCheck> findByCategory_Name(String categoryName, Pageable pageable);

  /**
   * Finds quality checks that have no category assigned.
   *
   * @param pageable pagination and sorting information
   * @return a page of quality checks without a category
   */
  Page<QualityCheck> findByCategoryIsNull(Pageable pageable);
}
