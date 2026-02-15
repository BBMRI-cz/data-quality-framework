package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import org.springframework.data.jpa.repository.JpaRepository;

interface QualityCheckRepository extends JpaRepository<QualityCheck, Long> {}
