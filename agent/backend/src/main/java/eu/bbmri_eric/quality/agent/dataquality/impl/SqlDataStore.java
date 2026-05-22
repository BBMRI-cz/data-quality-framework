package eu.bbmri_eric.quality.agent.dataquality.impl;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.dto.DatabaseHealthDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.DBStatus;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

class SqlDataStore implements DataStore {
  private static final String NOT_IMPLEMENTED_MESSAGE = "SQL data store not implemented yet.";

  public SqlDataStore() {
  }

  @Override
  public JSONObject getEntity(String entityType, String id) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override
  public JSONObject checkHealth() {
    JSONObject response = new JSONObject();
    response.put("status", "DOWN");
    response.put("details", new JSONObject().put("error", NOT_IMPLEMENTED_MESSAGE));
    return response;
  }

  @Override
  public ResultDTO executeQuery(String query) {
    return new ResultDTO(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override
  public DatabaseHealthDTO checkHealthV2() {
    return new DatabaseHealthDTO(DBStatus.DOWN, NOT_IMPLEMENTED_MESSAGE, null);
  }
}

