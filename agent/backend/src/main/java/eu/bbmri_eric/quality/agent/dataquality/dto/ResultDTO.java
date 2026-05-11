package eu.bbmri_eric.quality.agent.dataquality.dto;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Result of a Data Quality Check
 *
 * @param rawResult unobfuscated result of the check
 * @param entityType type of record that was counted
 * @param idSet set of entity IDs that matched
 * @param error error message thrown when the check was executed
 */
public record ResultDTO(Integer rawResult, String entityType, Set<String> idSet, String error) {
  public ResultDTO(Integer rawResult, String entityType, Set<String> idSet) {
    this(rawResult, entityType, idSet, null);
  }

  public ResultDTO(String error) {
    this(null, "", Collections.emptySet(), error);
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
