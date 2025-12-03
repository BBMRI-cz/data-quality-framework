package eu.bbmri_eric.quality.agent.dataquality.dto;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public record ResultDTO(Integer rawResult, String entityType, Set<String> idSet, String error) {
  public ResultDTO(Integer rawResult, String entityType, Set<String> idSet) {
    this(rawResult, entityType, idSet, null);
  }

  public ResultDTO(String error) {
    this(-1, "", Collections.emptySet(), error);
  }

  public static ResultDTO resultFromIdPaths(Set<String> idPaths, String entityName) {
    Set<String> idSet =
        idPaths.stream()
            .map(
                path -> {
                  String[] split = path.split("/");
                  return split[split.length - 1];
                })
            .collect(Collectors.toSet());
    return new ResultDTO(idPaths.size(), entityName, idSet);
  }
}
