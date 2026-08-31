package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for managing QualityCheck entities. */
@Repository
interface QualityCheckRepository extends JpaRepository<QualityCheck, Long> {

  Optional<QualityCheck> findByHash(String hash);

  Optional<QualityCheck> findByVersions_Hash(String hash);
}
