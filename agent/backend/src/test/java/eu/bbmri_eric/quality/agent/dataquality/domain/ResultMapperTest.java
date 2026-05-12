package eu.bbmri_eric.quality.agent.dataquality.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.quality.agent.dataquality.config.ResultMapper;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

public class ResultMapperTest {

  private final ModelMapper modelMapper = new ModelMapper();
  private final ResultMapper resultMapper = new ResultMapper(modelMapper);

  @BeforeEach
  void setUp() {
    resultMapper.addMappings();
  }

  @Test
  void mapResultDTO_withAllFields_mapsIntoResult() {
    Set<String> idSet = Set.of("patient-1", "patient-2", "patient-3");
    ResultDTO resultDTO = new ResultDTO(100, "Patient", idSet, "No errors");

    Result result = modelMapper.map(resultDTO, Result.class);

    assertEquals(100, result.getRawValue());
    assertEquals(idSet.size(), result.getPatients().size());
    assertTrue(result.getPatients().containsAll(idSet));
    assertEquals("", result.getCheckName());
    assertNull(result.getCheckId());
  }

  @Test
  void mapResultDTO_withNullError_keepsErrorNull() {
    Set<String> idSet = Set.of("patient-1", "patient-2");
    ResultDTO resultDTO = new ResultDTO(50, "Procedure", idSet, null);

    Result result = modelMapper.map(resultDTO, Result.class);

    assertEquals(50, result.getRawValue());
    assertEquals(idSet.size(), result.getPatients().size());
    assertNull(result.getError());
  }

  @Test
  void mapResultDTO_withEmptyIdSet_mapsToEmptyPatientsCollection() {
    Set<String> emptyIdSet = new HashSet<>();
    ResultDTO resultDTO = new ResultDTO(0, "Observation", emptyIdSet, null);

    Result result = modelMapper.map(resultDTO, Result.class);

    assertEquals(0, result.getRawValue());
    assertEquals(0, result.getPatients().size());
    assertTrue(result.getPatients().isEmpty());
  }

  @Test
  void mapResultDTO_withNullRawResult_keepsRawValueNull() {
    Set<String> idSet = Set.of("patient-1");
    ResultDTO resultDTO = new ResultDTO(null, "MissingCount", idSet, null);

    Result result = modelMapper.map(resultDTO, Result.class);

    assertNull(result.getRawValue());
    assertEquals(idSet, result.getPatients());
  }

  @Test
  void mapResultDTO_withNullIdSet_setsPatientsToNull() {
    ResultDTO resultDTO = new ResultDTO(12, "UnknownPatients", null, null);

    Result result = modelMapper.map(resultDTO, Result.class);

    assertEquals(12, result.getRawValue());
    assertNull(result.getPatients());
  }

  @Test
  void mapResultDTO_withUnusualPatientIds_preservesExactValues() {
    Set<String> idSet = Set.of("patient-1", "患者-2", "ID/3", " leading-space");
    ResultDTO resultDTO = new ResultDTO(4, "UnicodeAndSymbols", idSet, null);

    Result result = modelMapper.map(resultDTO, Result.class);

    assertEquals(idSet, result.getPatients());
    assertEquals(4, result.getRawValue());
  }

  @Test
  void mapResultDTO_thenQualityCheck_mapsIntoExistingResult() {
    Set<String> idSet = Set.of("patient-1");
    ResultDTO resultDTO = new ResultDTO(9, "ExistingTarget", idSet, null);

    Result result = new Result("Preexisting Check", 42L, null, null, 10, 20, 1.0f, null, null);

    modelMapper.map(resultDTO, result);

    QualityCheck qualityCheck =
        new QualityCheck(123L, "Age Check", "Checks ages", "define Age: true");
    qualityCheck.setWarningThreshold(17);
    qualityCheck.setErrorThreshold(31);
    qualityCheck.setEpsilonBudget(0.75f);

    modelMapper.map(qualityCheck, result);

    assertEquals(9, result.getRawValue());
    assertEquals(idSet, result.getPatients());
    assertNull(result.getId());
    assertEquals(123L, result.getCheckId());
    assertEquals("Age Check", result.getCheckName());
    assertEquals(17, result.getWarningThreshold());
    assertEquals(31, result.getErrorThreshold());
    assertEquals(0.75f, result.getEpsilon());
  }

  @Test
  void mapQualityCheck_doesNotMapErrorThresholdIntoErrorMessage() {
    Result result = new Result("Preexisting Check", 42L, null, null, 10, 20, 1.0f, null, null);

    QualityCheck qualityCheck =
        new QualityCheck(123L, "Age Check", "Checks ages", "define Age: true");
    qualityCheck.setWarningThreshold(17);
    qualityCheck.setErrorThreshold(31);
    qualityCheck.setEpsilonBudget(0.75f);

    modelMapper.map(qualityCheck, result);

    assertNull(result.getError());
    assertEquals(17, result.getWarningThreshold());
    assertEquals(31, result.getErrorThreshold());
    assertEquals(0.75f, result.getEpsilon());
  }

  @Test
  void mapResultDTO_preservesUnmappedFieldsOnNewResult() {
    Set<String> idSet = Set.of("patient-zero");
    ResultDTO resultDTO = new ResultDTO(0, "MissingData", idSet, "Data check failed");

    Result result = modelMapper.map(resultDTO, Result.class);

    assertEquals(0, result.getRawValue());
    assertEquals(1, result.getPatients().size());
    assertTrue(result.getPatients().contains("patient-zero"));
    assertEquals("", result.getCheckName());
    assertNull(result.getCheckId());
    assertNull(result.getObfuscatedValue());
  }

  @Test
  void mapResultDTO_doesNotModifyOriginalInput() {
    Set<String> idSet = Set.of("patient-a", "patient-b");
    ResultDTO resultDTO = new ResultDTO(7, "Sample", idSet, "error message");

    Result result = modelMapper.map(resultDTO, Result.class);

    assertEquals(7, result.getRawValue());
    assertEquals(idSet, result.getPatients());
    assertNull(result.getError());
    assertEquals(7, resultDTO.rawResult());
    assertEquals("Sample", resultDTO.entityType());
    assertEquals(idSet, resultDTO.idSet());
    assertEquals("error message", resultDTO.error());
  }
}
