package eu.bbmri_eric.quality.agent.dataquality.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DataQualityCheckTest {
  @Test
  void testConstructorInitializesFields() {
    Long id = 1L;
    String name = "Null check";
    String description = "Checks for null values";
    String query = "SELECT COUNT(*) FROM my_table WHERE col IS NULL";

    QualityCheck check = new QualityCheck(id, name, description, query);

    assertEquals(id, check.getId());
    assertEquals(name, check.getName());
    assertEquals(description, check.getDescription());
    assertEquals(query, check.getQuery());
  }

  @Test
  void testSettersAndGetters() {
    QualityCheck check = new QualityCheck();

    check.setId(100L);
    check.setName("Test DataQualityCheck");
    check.setDescription("Just testing");
    check.setQuery("SELECT * FROM test");

    assertEquals(100L, check.getId());
    assertEquals("Test DataQualityCheck", check.getName());
    assertEquals("Just testing", check.getDescription());
    assertEquals("SELECT * FROM test", check.getQuery());
  }

  @Test
  void testExecuteDoesNotThrow() {
    QualityCheck check = new QualityCheck();
    assertDoesNotThrow(() -> check.execute(null));
  }
}
