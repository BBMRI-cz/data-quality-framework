package eu.bbmri_eric.quality.agent.check;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

record Result(Integer numberOfEntities, String entityType, Set<String> idSet, String error) {
  Result(Integer numberOfEntities, String entityType, Set<String> idSet) {
    this(numberOfEntities, entityType, idSet, null);
  }

  Result(String error) {
    this(-1, "", Collections.emptySet(), error);
  }

  public static Result resultFromIdPaths(Set<String> idPaths, String entityName) {
    Set<String> idSet =
        idPaths.stream()
            .map(
                path -> {
                  String[] split = path.split("/");
                  return split[split.length - 1];
                })
            .collect(Collectors.toSet());
    return new Result(idPaths.size(), entityName, idSet);
  }
}
