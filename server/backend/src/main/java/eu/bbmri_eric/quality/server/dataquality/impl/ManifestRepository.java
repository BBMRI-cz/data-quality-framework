package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.dataquality.domain.Manifest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for managing Manifest entities. */
@Repository
interface ManifestRepository extends JpaRepository<Manifest, Long> {}
