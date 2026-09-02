package eu.bbmri_eric.quality.server.dataquality.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.crypto.KeyProvider;
import eu.bbmri_eric.quality.server.crypto.SignatureException;
import eu.bbmri_eric.quality.server.crypto.SignatureService;
import eu.bbmri_eric.quality.server.dataquality.ManifestService;
import eu.bbmri_eric.quality.server.dataquality.domain.Manifest;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckVersion;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestBody;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ManifestDTO;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
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
    Manifest manifest = new Manifest(createDTO.getName(), "{}", null, null);
    Manifest persisted = manifestRepository.saveAndFlush(manifest);
    String body = buildBody(createDTO, persisted.getId());
    String signature = signBody(body);
    persisted.setBody(body);
    persisted.setSignature(signature);
    persisted.setKeyId(keyProvider.getKeyId());
    Manifest savedManifest = manifestRepository.save(persisted);
    return modelMapper.map(savedManifest, ManifestDTO.class);
  }

  private String signBody(String body) {
    try {
      byte[] signature = signatureService.sign(body.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(signature);
    } catch (GeneralSecurityException e) {
      throw new SignatureException("Failed to sign manifest body", e);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public ManifestDTO findById(Long id) {
    Objects.requireNonNull(id, "ID cannot be null");
    return modelMapper.map(
        manifestRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Manifest with ID %s not found".formatted(id))),
        ManifestDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ManifestDTO> findAll() {
    return manifestRepository.findAll().stream()
        .map(manifest -> modelMapper.map(manifest, ManifestDTO.class))
        .toList();
  }

  private String buildBody(ManifestCreateDTO createDTO, Long manifestId) {
    List<ManifestBody.Check> checks = new ArrayList<>();
    for (String hash : createDTO.getHashes()) {
      QualityCheckVersion version =
          qualityCheckVersionRepository
              .findByHash(hash)
              .orElseThrow(
                  () ->
                      new EntityNotFoundException(
                          "No quality check version found for hash: " + hash));
      checks.add(
          new ManifestBody.Check(
              String.valueOf(version.getQualityCheck().getId()), version.getVersion(), hash));
    }
    ManifestBody body = new ManifestBody(manifestId, Instant.now(), checks);
    try {
      return objectMapper.writeValueAsString(body);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to serialize manifest body", e);
    }
  }
}
