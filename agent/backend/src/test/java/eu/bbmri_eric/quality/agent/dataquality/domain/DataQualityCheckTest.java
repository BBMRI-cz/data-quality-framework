package eu.bbmri_eric.quality.agent.dataquality.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import java.util.Set;
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
    DataStore dataStore =
        new DataStore() {
          @Override
          public org.json.JSONObject getEntity(String entityType, String id) {
            return new org.json.JSONObject();
          }

          @Override
          public org.json.JSONObject checkHealth() {
            return new org.json.JSONObject();
          }

          @Override
          public ResultDTO executeQuery(String query) {
            return new ResultDTO(0, "", Set.of());
          }

          @Override
          public eu.bbmri_eric.quality.agent.dataquality.dto.DatabaseHealthDTO checkHealthV2() {
            return null;
          }
        };
    assertDoesNotThrow(() -> check.execute(dataStore));
  }
}
