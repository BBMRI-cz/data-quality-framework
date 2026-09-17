package eu.bbmri_eric.quality.agent.server;

import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.server.dto.ManifestDownloadDto;
import eu.bbmri_eric.quality.agent.server.dto.ManifestDto;
import java.util.List;

/**
 * Service for working with quality check manifests published by registered central servers.
 *
 * <p>The fetch operations proxy live requests to the central server; nothing is persisted on the
 * agent until a manifest is explicitly downloaded.
 */
public interface ManifestService {

  /**
   * Fetches all manifests published by a registered central server without downloading them.
   *
   * @param serverId the ID of the registered server
   * @return the published manifests
   * @throws jakarta.persistence.EntityNotFoundException if no server exists with the given ID
   * @throws ServerCommunicationException if the manifests cannot be retrieved from the server
   */
  List<ManifestDto> fetchManifests(String serverId);

  /**
   * Fetches a single manifest, including its signed versions, from a registered central server
   * without downloading it.
   *
   * @param serverId the ID of the registered server
   * @param manifestId the ID of the manifest on the central server
   * @return the manifest with its versions
   * @throws jakarta.persistence.EntityNotFoundException if no server exists with the given ID
   * @throws ServerCommunicationException if the manifest cannot be retrieved from the server
   */
  ManifestDto fetchManifest(String serverId, Long manifestId);

  /**
   * Fetches the quality checks pinned by a specific manifest version from a registered central
   * server without persisting them.
   *
   * @param serverId the ID of the registered server
   * @param manifestId the ID of the manifest on the central server
   * @param versionId the ID of the manifest version on the central server
   * @return the quality checks referenced by the manifest version, mapped to agent DTOs
   * @throws jakarta.persistence.EntityNotFoundException if no server exists with the given ID
   * @throws ServerCommunicationException if the quality checks cannot be retrieved from the server
   */
  List<QualityCheckDTO> fetchVersionQualityChecks(String serverId, Long manifestId, Long versionId);

  /**
   * Downloads a manifest version from a registered central server and persists it together with its
   * quality checks as local quality checks.
   *
   * @param serverId the ID of the registered server
   * @param manifestId the ID of the manifest on the central server
   * @param version the version number to download
   * @return the result of the download
   * @throws jakarta.persistence.EntityNotFoundException if no server exists with the given ID or
   *     the manifest has no such version
   * @throws ServerCommunicationException if the manifest cannot be retrieved from the server
   */
  ManifestDownloadDto downloadManifest(String serverId, Long manifestId, int version);
}
