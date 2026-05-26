package eu.bbmri_eric.quality.agent.settings;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DatabaseTypeTest {

  @Test
  void fromString_withSql_shouldReturnSqlType() {
    DatabaseType result = DatabaseType.fromString("SQL");
    assertEquals(DatabaseType.SQL, result);
  }

  @Test
  void fromString_withInvalidValue_shouldThrowException() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> DatabaseType.fromString("INVALID"));

    assertTrue(exception.getMessage().contains("Unknown database type"));
    assertTrue(exception.getMessage().contains("INVALID"));
  }

  @Test
  void fromString_withNull_shouldThrowException() {
    assertThrows(IllegalArgumentException.class, () -> DatabaseType.fromString(null));
  }

  @Test
  void fromString_withEmptyString_shouldThrowException() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> DatabaseType.fromString(""));

    assertTrue(exception.getMessage().contains("Unknown database type"));
  }

  @Test
  void getValue_forFhir_shouldReturnFhirString() {
    String result = DatabaseType.FHIR.getValue();
    assertEquals("FHIR", result);
  }

  @Test
  void allValues_shouldHaveTwoTypes() {
    DatabaseType[] types = DatabaseType.values();
    assertEquals(2, types.length);
  }

  @Test
  void valueOf_withFhir_shouldReturnFhirType() {
    DatabaseType result = DatabaseType.valueOf("FHIR");
    assertEquals(DatabaseType.FHIR, result);
  }
}
