package eu.bbmri_eric.quality.agent.server;

import eu.bbmri_eric.quality.agent.dataquality.dto.ObfuscatedReportDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.server.dto.ManifestDto;
import java.util.List;

public interface CentralServerClient {
  RegistrationCredentials register() throws Exception;

  ServerConnectionStatus checkRegistrationStatus() throws Exception;

  void healthCheck();

  void updateAgentVersion(String version) throws Exception;

  void sendReport(ObfuscatedReportDTO reportDTO) throws Exception;

  /**
   * Fetches all quality check manifests published by the central server.
   *
   * @return the published manifests
   * @throws ServerCommunicationException if the manifests cannot be retrieved from the server
   */
  List<ManifestDto> getManifests();

  /**
   * Fetches a single quality check manifest, including its published versions.
   *
   * @param manifestId the ID of the manifest on the central server
   * @return the manifest with its versions
   * @throws ServerCommunicationException if the manifest cannot be retrieved from the server
   */
  ManifestDto getManifest(Long manifestId);

  /**
   * Fetches the quality checks pinned by a specific manifest version, mapped to agent quality check
   * DTOs.
   *
   * @param manifestId the ID of the manifest on the central server
   * @param versionId the ID of the manifest version on the central server
   * @return the quality checks referenced by the manifest version
   * @throws ServerCommunicationException if the quality checks cannot be retrieved from the server
   */
  List<QualityCheckDTO> getManifestVersionQualityChecks(Long manifestId, Long versionId);
}
