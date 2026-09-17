package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.server.domain.Manifest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ManifestRepository extends JpaRepository<Manifest, String> {

  Optional<Manifest> findByServerIdAndRemoteId(String serverId, Long remoteId);
}
