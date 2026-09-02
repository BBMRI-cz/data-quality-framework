package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckVersion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for managing QualityCheckVersion entities. */
@Repository
interface QualityCheckVersionRepository extends JpaRepository<QualityCheckVersion, Long> {

  /**
   * Finds a quality check version by its SHA-256 hash.
   *
   * @param hash the hash to search for
   * @return an Optional containing the version if found
   */
  Optional<QualityCheckVersion> findByHash(String hash);
}
