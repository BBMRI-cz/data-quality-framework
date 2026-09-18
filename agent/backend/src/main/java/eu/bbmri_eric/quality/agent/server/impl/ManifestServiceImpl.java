package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckCreateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.server.CentralServerClient;
import eu.bbmri_eric.quality.agent.server.CentralServerClientFactory;
import eu.bbmri_eric.quality.agent.server.ManifestService;
import eu.bbmri_eric.quality.agent.server.ManifestSignatureVerifier;
import eu.bbmri_eric.quality.agent.server.ServerCommunicationException;
import eu.bbmri_eric.quality.agent.server.domain.Manifest;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.dto.ManifestDownloadDto;
import eu.bbmri_eric.quality.agent.server.dto.ManifestDto;
import eu.bbmri_eric.quality.agent.server.dto.ManifestVersionDto;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Default implementation of {@link ManifestService}. */
@Service
@Transactional(readOnly = true)
class ManifestServiceImpl implements ManifestService {

  private static final Logger log = LoggerFactory.getLogger(ManifestServiceImpl.class);
  private static final String FALLBACK_DESCRIPTION = "No description provided";

  private final ServerRepository serverRepository;
  private final ManifestRepository manifestRepository;
  private final CentralServerClientFactory clientFactory;
  private final SettingsService settingsService;
  private final QualityCheckService qualityCheckService;
  private final ManifestSignatureVerifier signatureVerifier;

  ManifestServiceImpl(
      ServerRepository serverRepository,
      ManifestRepository manifestRepository,
      CentralServerClientFactory clientFactory,
      SettingsService settingsService,
      QualityCheckService qualityCheckService,
      ManifestSignatureVerifier signatureVerifier) {
    this.serverRepository = serverRepository;
    this.manifestRepository = manifestRepository;
    this.clientFactory = clientFactory;
    this.settingsService = settingsService;
    this.qualityCheckService = qualityCheckService;
    this.signatureVerifier = signatureVerifier;
  }

  @Override
  public List<ManifestDto> fetchManifests(String serverId) {
    return createClient(loadServer(serverId)).getManifests();
  }

  @Override
  public ManifestDto fetchManifest(String serverId, Long manifestId) {
    return createClient(loadServer(serverId)).getManifest(manifestId);
  }

  @Override
  public List<QualityCheckDTO> fetchVersionQualityChecks(
      String serverId, Long manifestId, Long versionId) {
    return createClient(loadServer(serverId))
        .getManifestVersionQualityChecks(manifestId, versionId);
  }

  @Override
  @Transactional
  public ManifestDownloadDto downloadManifest(String serverId, Long manifestId, int version) {
    Server server = loadServer(serverId);
    CentralServerClient client = createClient(server);
    ManifestDto remoteManifest = client.getManifest(manifestId);
    ManifestVersionDto versionDto = resolveRemoteVersion(remoteManifest, version);
    signatureVerifier.verify(server.getPublicKey(), versionDto);
    List<QualityCheckDTO> checks =
        client.getManifestVersionQualityChecks(manifestId, versionDto.getRemoteId());
    rejectUnsupportedTypes(checks, version);
    Manifest manifest =
        manifestRepository
            .findByServerIdAndRemoteId(serverId, manifestId)
            .orElseGet(() -> new Manifest(manifestId, remoteManifest.getName(), server));
    manifest.setName(remoteManifest.getName());
    manifest.setInstalledVersion(version);
    replaceInstalledChecks(manifest, checks);
    Manifest saved = manifestRepository.save(manifest);
    log.info(
        "Downloaded manifest {} version {} from server {} with {} quality checks",
        manifestId,
        version,
        serverId,
        checks.size());
    return new ManifestDownloadDto(
        saved.getId(), saved.getRemoteId(), saved.getName(), version, checks.size());
  }

  private ManifestVersionDto resolveRemoteVersion(ManifestDto manifest, int version) {
    return manifest.getVersions().stream()
        .filter(candidate -> candidate.getVersion() == version)
        .findFirst()
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Manifest version %d not found for manifest %d"
                        .formatted(version, manifest.getRemoteId())));
  }

  private void rejectUnsupportedTypes(List<QualityCheckDTO> checks, int version) {
    List<String> unsupported =
        checks.stream()
            .filter(check -> check.getType() == null)
            .map(QualityCheckDTO::getName)
            .toList();
    if (!unsupported.isEmpty()) {
      throw new ServerCommunicationException(
          "Manifest version %d contains quality checks with an unsupported query type and was rejected: %s"
              .formatted(version, String.join(", ", unsupported)));
    }
  }

  /**
   * Reconciles the manifest's installed checks with the freshly downloaded set, so repeated or
   * newer downloads replace the previous installation instead of accumulating duplicates.
   */
  private void replaceInstalledChecks(Manifest manifest, List<QualityCheckDTO> checks) {
    for (Long checkId : List.copyOf(manifest.getQualityCheckIds())) {
      try {
        qualityCheckService.delete(checkId);
      } catch (eu.bbmri_eric.quality.agent.common.exception.EntityNotFoundException e) {
        log.debug("Previously installed check {} was already removed", checkId);
      }
      manifest.removeQualityCheckId(checkId);
    }
    for (QualityCheckDTO check : checks) {
      QualityCheckDTO created = qualityCheckService.create(toCreateDto(check));
      manifest.addQualityCheckId(created.getId());
    }
  }

  private QualityCheckCreateDTO toCreateDto(QualityCheckDTO check) {
    String description = check.getDescription();
    return new QualityCheckCreateDTO(
        check.getName(),
        description == null || description.isBlank() ? FALLBACK_DESCRIPTION : description,
        check.getQuery(),
        check.getType(),
        check.getWarningThreshold(),
        check.getErrorThreshold(),
        check.getEpsilonBudget());
  }

  /**
   * Loads the registered server the manifest belongs to.
   *
   * @param serverId the ID of the registered server
   * @return the server
   */
  private Server loadServer(String serverId) {
    return serverRepository
        .findById(serverId)
        .orElseThrow(() -> new EntityNotFoundException("Server not found with id: " + serverId));
  }

  /**
   * Creates an authenticated client for the registered server.
   *
   * @param server the registered server
   * @return a configured client for that server
   */
  private CentralServerClient createClient(Server server) {
    String agentId = settingsService.getSettings().getAgentId();
    return clientFactory.createClient(
        agentId, server.getUrl(), server.getClientId(), server.getClientSecret());
  }
}
