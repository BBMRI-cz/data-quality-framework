package eu.bbmri_eric.quality.agent.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuditDiffTest {

  @Test
  void describe_withChangedValues_listsOnlyChangesInOrder() {
    Map<String, Object> before = new LinkedHashMap<>();
    before.put("epsilon", 1.0);
    before.put("fhirUrl", "http://localhost/fhir");
    before.put("noiseMechanism", "GAUSSIAN");
    Map<String, Object> after = new LinkedHashMap<>(before);
    after.put("epsilon", 0.5);
    after.put("noiseMechanism", "LAPLACE");

    assertThat(AuditDiff.describe(before, after, Set.of()))
        .isEqualTo("epsilon from 1.0 to 0.5; noiseMechanism from GAUSSIAN to LAPLACE");
  }

  @Test
  void describe_withoutChanges_returnsEmpty() {
    Map<String, Object> state = Map.of("epsilon", 1.0);

    assertThat(AuditDiff.describe(state, Map.copyOf(state), Set.of())).isEmpty();
  }

  @Test
  void describe_withSensitiveFields_hidesTheirValues() {
    Map<String, Object> before = Map.of("token", "old-token", "sqlPassword", "old-secret");
    Map<String, Object> after = Map.of("token", "new-token", "sqlPassword", "new-secret");

    String diff = AuditDiff.describe(before, after, Set.of("token"));

    assertThat(diff)
        .contains("token changed")
        .contains("sqlPassword changed")
        .doesNotContain("old-token", "new-token", "old-secret", "new-secret");
  }

  @Test
  void describe_withNullOrBlankValues_showsEmptyMarker() {
    Map<String, Object> before = new HashMap<>();
    before.put("sqlUrl", null);
    before.put("sqlUsername", "");
    Map<String, Object> after = new HashMap<>();
    after.put("sqlUrl", "jdbc:postgresql://db/omop");

    assertThat(AuditDiff.describe(before, after, Set.of()))
        .contains("sqlUrl from (empty) to jdbc:postgresql://db/omop")
        .doesNotContain("sqlUsername");
  }

  @Test
  void describe_withAddedAndRemovedFields_includesBoth() {
    Map<String, Object> before = new LinkedHashMap<>();
    before.put("removed", "x");
    Map<String, Object> after = new LinkedHashMap<>();
    after.put("added", "y");

    assertThat(AuditDiff.describe(before, after, Set.of()))
        .isEqualTo("removed from x to (empty); added from (empty) to y");
  }

  @Test
  void describe_withNestedObjects_comparesTheirPropertiesByPath() {
    Map<String, Object> before = new LinkedHashMap<>();
    before.put("name", "Age check");
    before.put("category", Map.of("id", 1, "name", "Completeness"));
    Map<String, Object> after = new LinkedHashMap<>();
    after.put("name", "Age check");
    after.put("category", Map.of("id", 2, "name", "Accuracy"));

    assertThat(AuditDiff.describe(before, after, Set.of()))
        .contains("category.id from 1 to 2")
        .contains("category.name from Completeness to Accuracy")
        .doesNotContain("Age check");
  }

  @Test
  void describe_withNestedObjectAddedOrRemoved_listsItsProperties() {
    Map<String, Object> before = new HashMap<>();
    before.put("category", null);
    Map<String, Object> after = Map.of("category", Map.of("name", "Accuracy"));

    assertThat(AuditDiff.describe(before, after, Set.of()))
        .isEqualTo("category.name from (empty) to Accuracy");
    assertThat(AuditDiff.describe(after, before, Set.of()))
        .isEqualTo("category.name from Accuracy to (empty)");
  }

  @Test
  void describe_withSensitiveNestedProperty_hidesItsValue() {
    Map<String, Object> before = Map.of("server", Map.of("token", "old"));
    Map<String, Object> after = Map.of("server", Map.of("token", "new"));

    assertThat(AuditDiff.describe(before, after, Set.of("server.token")))
        .isEqualTo("server.token changed");
  }
}
