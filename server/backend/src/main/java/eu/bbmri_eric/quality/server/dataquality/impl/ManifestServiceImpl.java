package eu.bbmri_eric.quality.server.dataquality.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.server.common.EntityAlreadyExistsException;
import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.crypto.KeyProvider;
import eu.bbmri_eric.quality.server.crypto.SignatureException;
import eu.bbmri_eric.quality.server.crypto.SignatureService;
import eu.bbmri_eric.quality.server.dataquality.ManifestService;
import eu.bbmri_eric.quality.server.dataquality.domain.Manifest;
import eu.bbmri_eric.quality.server.dataquality.domain.ManifestVersion;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckVersion;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestBody;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestVersionCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestVersionDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDetailedDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckVersionDTO;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service implementation for managing quality check manifests. */
@Service
@Transactional
class ManifestServiceImpl implements ManifestService {

  private final ManifestRepository manifestRepository;
  private final QualityCheckVersionRepository qualityCheckVersionRepository;
  private final ModelMapper modelMapper;
  private final ObjectMapper objectMapper;
  private final SignatureService signatureService;
  private final KeyProvider keyProvider;

  public ManifestServiceImpl(
      ManifestRepository manifestRepository,
      QualityCheckVersionRepository qualityCheckVersionRepository,
      ModelMapper modelMapper,
      ObjectMapper objectMapper,
      SignatureService signatureService,
      KeyProvider keyProvider) {
    this.manifestRepository = manifestRepository;
    this.qualityCheckVersionRepository = qualityCheckVersionRepository;
    this.modelMapper = modelMapper;
    this.objectMapper = objectMapper;
    this.signatureService = signatureService;
    this.keyProvider = keyProvider;
  }

  @Override
  public ManifestDTO create(ManifestCreateDTO createDTO) {
    Objects.requireNonNull(createDTO, "ManifestCreateDTO cannot be null");
    Manifest manifest = new Manifest(createDTO.getName());
    Manifest savedManifest = manifestRepository.save(manifest);
    return modelMapper.map(savedManifest, ManifestDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public ManifestDTO findById(Long id) {
    return modelMapper.map(findManifest(id), ManifestDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ManifestDTO> findAll() {
    return manifestRepository.findAll().stream()
        .map(manifest -> modelMapper.map(manifest, ManifestDTO.class))
        .toList();
  }

  @Override
  public ManifestVersionDTO createVersion(Long id, ManifestVersionCreateDTO createDTO) {
    Objects.requireNonNull(createDTO, "ManifestVersionCreateDTO cannot be null");
    Manifest manifest = findManifest(id);

    int versionNumber = resolveVersion(createDTO.getVersion(), manifest);
    boolean versionExists =
        manifest.getVersions().stream().anyMatch(v -> v.getVersion() == versionNumber);
    if (versionExists) {
      throw new EntityAlreadyExistsException(
          "Version %d already exists for manifest with ID: %d".formatted(versionNumber, id));
    }

    List<QualityCheckVersion> qualityCheckVersions =
        resolveQualityCheckVersions(createDTO.getHashes());

    ManifestVersion version =
        new ManifestVersion(manifest, versionNumber, "", "", keyProvider.getKeyId());
    version.addQualityChecks(qualityCheckVersions);
    version.setBody(buildBody(qualityCheckVersions, version));
    version.setSignature(signBody(version.getBody()));
    manifest.addVersion(version);
    manifestRepository.save(manifest);
    return modelMapper.map(version, ManifestVersionDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ManifestVersionDTO> findVersions(Long id) {
    Manifest manifest = findManifest(id);
    return manifest.getVersions().stream()
        .sorted(Comparator.comparingInt(ManifestVersion::getVersion))
        .map(version -> modelMapper.map(version, ManifestVersionDTO.class))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<QualityCheckDetailedDTO> findVersionQualityChecks(Long manifestId, Long versionId) {
    ManifestVersion manifestVersion = findManifestVersion(manifestId, versionId);
    return manifestVersion.getQualityChecks().stream().map(this::toDetailedDto).toList();
  }

  private ManifestVersion findManifestVersion(Long manifestId, Long versionId) {
    Objects.requireNonNull(versionId, "Version ID cannot be null");
    Manifest manifest = findManifest(manifestId);
    return manifest.getVersions().stream()
        .filter(version -> version.getId().equals(versionId))
        .findFirst()
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Manifest version with ID %s not found for manifest with ID %s"
                        .formatted(versionId, manifestId)));
  }

  private List<QualityCheckVersion> resolveQualityCheckVersions(List<String> hashes) {
    return hashes.stream()
        .map(
            hash ->
                qualityCheckVersionRepository
                    .findByHash(hash)
                    .orElseThrow(
                        () ->
                            new EntityNotFoundException(
                                "No quality check version found for hash: " + hash)))
        .toList();
  }

  /**
   * Maps a specific quality check version to a detailed quality check DTO, restricting the versions
   * to the one referenced by the manifest version.
   */
  private QualityCheckDetailedDTO toDetailedDto(QualityCheckVersion version) {
    QualityCheckDetailedDTO dto =
        modelMapper.map(version.getQualityCheck(), QualityCheckDetailedDTO.class);
    dto.setVersions(List.of(modelMapper.map(version, QualityCheckVersionDTO.class)));
    return dto;
  }

  private Manifest findManifest(Long id) {
    Objects.requireNonNull(id, "ID cannot be null");
    return manifestRepository
        .findById(id)
        .orElseThrow(
            () -> new EntityNotFoundException("Manifest with ID %s not found".formatted(id)));
  }

  private int resolveVersion(Integer requestedVersion, Manifest manifest) {
    return requestedVersion != null
        ? requestedVersion
        : manifest.getVersions().stream()
                .map(ManifestVersion::getVersion)
                .max(Comparator.naturalOrder())
                .orElse(0)
            + 1;
  }

  private String buildBody(
      List<QualityCheckVersion> qualityCheckVersions, ManifestVersion manifestVersion) {
    List<ManifestBody.Check> checks = new ArrayList<>();
    for (QualityCheckVersion version : qualityCheckVersions) {
      checks.add(
          new ManifestBody.Check(
              String.valueOf(version.getQualityCheck().getId()),
              version.getVersion(),
              version.getHash()));
    }
    ManifestBody body =
        new ManifestBody(
            manifestVersion.getManifest().getId(), manifestVersion.getGeneratedAt(), checks);
    try {
      return objectMapper.writeValueAsString(body);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to serialize manifest body", e);
    }
  }

  private String signBody(String body) {
    try {
      byte[] signature = signatureService.sign(body.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(signature);
    } catch (GeneralSecurityException e) {
      throw new SignatureException("Failed to sign manifest body", e);
    }
  }
}
