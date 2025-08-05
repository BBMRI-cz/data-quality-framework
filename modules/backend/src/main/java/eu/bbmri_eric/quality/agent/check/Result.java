package eu.bbmri_eric.quality.agent.check;

import java.util.Collections;
import java.util.List;

record Result(Integer numberOfEntities, String entityType, List<String> idList, String error) {
  Result(Integer numberOfEntities, String entityType, List<String> idList) {
    this(numberOfEntities, entityType, idList,null);
  }

  Result(String error) {
    this(-1, "", Collections.emptyList(), error);
  }
}