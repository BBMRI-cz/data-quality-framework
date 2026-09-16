package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.ManifestService;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.dto.ManifestDto;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Default implementation of {@link ManifestService}. */
@Service
@Transactional(readOnly = true)
class ManifestServiceImpl implements ManifestService {

  private final ServerRepository serverRepository;
  private final CentralServerClientFactory clientFactory;
  private final SettingsService settingsService;

  ManifestServiceImpl(
      ServerRepository serverRepository,
      CentralServerClientFactory clientFactory,
      SettingsService settingsService) {
    this.serverRepository = serverRepository;
    this.clientFactory = clientFactory;
    this.settingsService = settingsService;
  }

  @Override
  public List<ManifestDto> fetchManifests(String serverId) {
    return createClient(serverId).getManifests();
  }

  @Override
  public ManifestDto fetchManifest(String serverId, Long manifestId) {
    return createClient(serverId).getManifest(manifestId);
  }

  @Override
  public List<QualityCheckDTO> fetchVersionQualityChecks(
      String serverId, Long manifestId, Long versionId) {
    return createClient(serverId).getManifestVersionQualityChecks(manifestId, versionId);
  }

  /**
   * Creates an authenticated client for the registered server.
   *
   * @param serverId the ID of the registered server
   * @return a configured client for that server
   */
  private CentralServerClient createClient(String serverId) {
    Server server =
        serverRepository
            .findById(serverId)
            .orElseThrow(
                () -> new EntityNotFoundException("Server not found with id: " + serverId));
    String agentId = settingsService.getSettings().getAgentId();
    return clientFactory.createClient(
        agentId, server.getUrl(), server.getClientId(), server.getClientSecret());
  }
}
