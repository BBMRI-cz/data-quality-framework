package eu.bbmri_eric.quality.agent.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Enum representing the type of database connection configured for the agent. */
public enum DatabaseType {
  /** FHIR server connection. */
  FHIR("FHIR"),

  /** SQL database connection. */
  SQL("SQL");

  private final String value;

  DatabaseType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static DatabaseType fromString(String value) {
    for (DatabaseType databaseType : DatabaseType.values()) {
      if (databaseType.value.equalsIgnoreCase(value)) {
        return databaseType;
      }
    }
    throw new IllegalArgumentException("Unknown database type: " + value);
  }
}
