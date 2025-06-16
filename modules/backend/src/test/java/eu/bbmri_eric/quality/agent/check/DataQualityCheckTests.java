package eu.bbmri_eric.quality.agent.check;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DataQualityCheckTests {
  @Test
  void testConstructorInitializesFields() {
    Long id = 1L;
    String name = "Null check";
    String description = "Checks for null values";
    String query = "SELECT COUNT(*) FROM my_table WHERE col IS NULL";

    CQLCheck check = new CQLCheck(id, name, description, query);

    assertEquals(id, check.getId());
    assertEquals(name, check.getName());
    assertEquals(description, check.getDescription());
    assertEquals(query, check.getQuery());
  }

  @Test
  void testSettersAndGetters() {
    CQLCheck check = new CQLCheck();

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
    CQLCheck check = new CQLCheck();
    assertDoesNotThrow(() -> check.execute(null));
  }
}
