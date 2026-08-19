package eu.bbmri_eric.quality.server.dataquality.impl;

import static java.util.stream.Collectors.toSet;

import eu.bbmri_eric.quality.server.common.EntityAlreadyExistsException;
import eu.bbmri_eric.quality.server.common.EntityNotFoundException;
import eu.bbmri_eric.quality.server.dataquality.QualityCheckService;
import eu.bbmri_eric.quality.server.dataquality.domain.Category;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckKeyword;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckUpdateDTO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service implementation for managing quality checks. */
@Service
@Transactional
class QualityCheckServiceImpl implements QualityCheckService {

  private final QualityCheckRepository qualityCheckRepository;
  private final CategoryRepository categoryRepository;
  private final ModelMapper modelMapper;

  public QualityCheckServiceImpl(
      QualityCheckRepository qualityCheckRepository,
      CategoryRepository categoryRepository,
      ModelMapper modelMapper) {
    this.qualityCheckRepository = qualityCheckRepository;
    this.categoryRepository = categoryRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  public QualityCheckDTO create(QualityCheckCreateDTO createDTO) {
    String hash = generateHash(createDTO);
    if (qualityCheckRepository.existsById(hash)) {
      throw new EntityAlreadyExistsException(
          "Quality check with hash '" + hash + "' already exists");
    }

    QualityCheck qualityCheck =
        new QualityCheck(
            hash,
            createDTO.getName(),
            createDTO.getDescription(),
            createDTO.getQuery(),
            createDTO.getType(),
            createDTO.getWarningThreshold(),
            createDTO.getErrorThreshold(),
            null);
    setCategory(createDTO.getCategoryId(), qualityCheck);
    return modelMapper.map(qualityCheckRepository.save(qualityCheck), QualityCheckDTO.class);
  }

  private static String generateHash(QualityCheckCreateDTO createDTO) {
    return hashQuery(createDTO.getQuery());
  }

  private static String hashQuery(String query) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(query.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not found", e);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public QualityCheckDTO findById(String id) {
    QualityCheck qualityCheck =
        qualityCheckRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Quality check not found with ID: " + id));
    return modelMapper.map(qualityCheck, QualityCheckDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public List<QualityCheckDTO> findAll() {
    return qualityCheckRepository.findAll().stream()
        .map(qualityCheck -> modelMapper.map(qualityCheck, QualityCheckDTO.class))
        .toList();
  }

  @Override
  public QualityCheckDTO update(String id, QualityCheckUpdateDTO updateDTO) {
    QualityCheck qualityCheck =
        qualityCheckRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Quality check not found with ID: " + id));
    boolean queryChanged = !Objects.equals(qualityCheck.getQuery(), updateDTO.getQuery());

    qualityCheck.setName(updateDTO.getName());
    qualityCheck.setDescription(updateDTO.getDescription());
    qualityCheck.setQuery(updateDTO.getQuery());
    qualityCheck.setType(updateDTO.getType());
    qualityCheck.setWarningThreshold(updateDTO.getWarningThreshold());
    qualityCheck.setErrorThreshold(updateDTO.getErrorThreshold());
    setCategory(updateDTO, qualityCheck);

    if (queryChanged) {
      String newHash = hashQuery(updateDTO.getQuery());
      if (qualityCheckRepository.existsById(newHash)) {
        throw new EntityAlreadyExistsException(
            "Quality check with hash '" + newHash + "' already exists");
      }
      return recreateWithNewHash(qualityCheck, newHash);
    }

    return modelMapper.map(qualityCheckRepository.save(qualityCheck), QualityCheckDTO.class);
  }

  private QualityCheckDTO recreateWithNewHash(QualityCheck qualityCheck, String newHash) {
    Set<String> keywords =
        qualityCheck.getKeywords().stream().map(QualityCheckKeyword::getKeyword).collect(toSet());
    Category category = qualityCheck.getCategory();

    qualityCheckRepository.delete(qualityCheck);
    qualityCheckRepository.flush();

    QualityCheck newCheck =
        new QualityCheck(
            newHash,
            qualityCheck.getName(),
            qualityCheck.getDescription(),
            qualityCheck.getQuery(),
            qualityCheck.getType(),
            qualityCheck.getWarningThreshold(),
            qualityCheck.getErrorThreshold(),
            category);
    newCheck.setKeywords(keywords);
    return modelMapper.map(qualityCheckRepository.save(newCheck), QualityCheckDTO.class);
  }

  private void setCategory(QualityCheckUpdateDTO updateDTO, QualityCheck qualityCheck) {
    setCategory(updateDTO.getCategoryId(), qualityCheck);
  }

  private void setCategory(Long categoryId, QualityCheck qualityCheck) {
    if (categoryId != null) {
      Category category =
          categoryRepository
              .findById(categoryId)
              .orElseThrow(
                  () -> new EntityNotFoundException("Category not found with ID: " + categoryId));
      qualityCheck.setCategory(category);
    } else {
      qualityCheck.setCategory(null);
    }
  }

  @Override
  public QualityCheckDTO setKeywords(String id, Set<String> keywords) {
    QualityCheck qualityCheck =
        qualityCheckRepository
            .findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException("Quality check not found with ID: " + id));

    qualityCheck.setKeywords(keywords);
    return modelMapper.map(qualityCheckRepository.save(qualityCheck), QualityCheckDTO.class);
  }
}
