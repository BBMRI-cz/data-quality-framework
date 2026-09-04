package eu.bbmri_eric.quality.server.dataquality.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class QualityCheckVersionTest {

  @Test
  void constructor_shouldDefaultTypeToUnknown() {
    QualityCheck qualityCheck = new QualityCheck("Test Check", "Description");

    QualityCheckVersion version = new QualityCheckVersion(qualityCheck, 1, "SELECT 1");

    assertEquals(QueryType.UNKNOWN, version.getType());
  }

  @Test
  void constructor_shouldSetProvidedType() {
    QualityCheck qualityCheck = new QualityCheck("Test Check", "Description");

    QualityCheckVersion version =
        new QualityCheckVersion(qualityCheck, 1, "SELECT 1", QueryType.SQL);

    assertEquals(QueryType.SQL, version.getType());
  }

  @Test
  void constructor_shouldDefaultTypeToUnknownWhenNullProvided() {
    QualityCheck qualityCheck = new QualityCheck("Test Check", "Description");

    QualityCheckVersion version =
        new QualityCheckVersion(qualityCheck, 1, "SELECT 1", (QueryType) null);

    assertEquals(QueryType.UNKNOWN, version.getType());
  }

  @Test
  void constructorWithHash_shouldDefaultTypeToUnknown() {
    QualityCheck qualityCheck = new QualityCheck("Test Check", "Description");

    QualityCheckVersion version = new QualityCheckVersion(qualityCheck, 1, "", "precomputed-hash");

    assertEquals(QueryType.UNKNOWN, version.getType());
    assertEquals("precomputed-hash", version.getHash());
  }

  @Test
  void constructorWithHash_shouldSetProvidedType() {
    QualityCheck qualityCheck = new QualityCheck("Test Check", "Description");

    QualityCheckVersion version =
        new QualityCheckVersion(qualityCheck, 1, "", "precomputed-hash", QueryType.CQL);

    assertEquals(QueryType.CQL, version.getType());
    assertEquals("precomputed-hash", version.getHash());
  }

  @Test
  void constructorWithHash_shouldDefaultTypeToUnknownWhenNullProvided() {
    QualityCheck qualityCheck = new QualityCheck("Test Check", "Description");

    QualityCheckVersion version =
        new QualityCheckVersion(qualityCheck, 1, "", "precomputed-hash", null);

    assertEquals(QueryType.UNKNOWN, version.getType());
  }

  @Test
  void queryType_shouldSupportAllExpectedValues() {
    assertDoesNotThrow(
        () -> {
          QueryType.valueOf("CQL");
          QueryType.valueOf("SQL");
          QueryType.valueOf("PYTHON");
          QueryType.valueOf("UNKNOWN");
        });
  }
}
