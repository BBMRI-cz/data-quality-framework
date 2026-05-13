package eu.bbmri_eric.quality.agent.dataquality.dto;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Result of a Data Quality Check */
public class ResultDTO {
  private Integer rawResult;
  private String entityType;
  private Set<String> idSet;
  private String error;

  /**
   * @param rawResult unobfuscated result of the check
   * @param entityType type of record that was counted
   * @param idSet set of entity IDs that matched
   * @param error error message thrown when the check was executed
   */
  public ResultDTO(Integer rawResult, String entityType, Set<String> idSet, String error) {
    this.rawResult = rawResult;
    this.entityType = entityType;
    this.idSet = idSet;
    this.error = error;
  }

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

  public Integer rawResult() {
    return rawResult;
  }

  public String entityType() {
    return entityType;
  }

  public Set<String> idSet() {
    return idSet;
  }

  public String error() {
    return error;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (ResultDTO) obj;
    return Objects.equals(this.rawResult, that.rawResult)
        && Objects.equals(this.entityType, that.entityType)
        && Objects.equals(this.idSet, that.idSet)
        && Objects.equals(this.error, that.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawResult, entityType, idSet, error);
  }

  @Override
  public String toString() {
    return "ResultDTO["
        + "rawResult="
        + rawResult
        + ", "
        + "entityType="
        + entityType
        + ", "
        + "idSet="
        + idSet
        + ", "
        + "error="
        + error
        + ']';
  }
}
