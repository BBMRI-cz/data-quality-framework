package eu.bbmri_eric.quality.server.dataquality.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupTest {

  private Group group;
  private Agent agent1;
  private Agent agent2;

  @BeforeEach
  void setUp() {
    group = new Group("Test Group");
    agent1 = new Agent("agent-1");
    agent2 = new Agent("agent-2");
  }

  @Test
  void constructor_createsGroupWithName() {
    var newGroup = new Group("Sample Group");
    assertEquals("Sample Group", newGroup.getName());
    assertNotNull(newGroup.getAgents());
    assertTrue(newGroup.getAgents().isEmpty());
  }

  @Test
  void setName_updatesGroupName() {
    group.setName("Updated Name");
    assertEquals("Updated Name", group.getName());
  }

  @Test
  void addAgent_addsAgentToGroup() {
    group.addAgent(agent1);
    assertEquals(1, group.getAgents().size());
    assertTrue(group.getAgents().contains(agent1));
  }

  @Test
  void addAgent_throwsExceptionWhenAgentIsNull() {
    var exception = assertThrows(IllegalArgumentException.class, () -> group.addAgent(null));
    assertEquals("Agent cannot be null", exception.getMessage());
  }

  @Test
  void addAgent_doesNotAddDuplicateAgent() {
    group.addAgent(agent1);
    group.addAgent(agent1);
    assertEquals(1, group.getAgents().size());
  }

  @Test
  void addAgent_addsMultipleAgents() {
    group.addAgent(agent1);
    group.addAgent(agent2);
    assertEquals(2, group.getAgents().size());
    assertTrue(group.getAgents().contains(agent1));
    assertTrue(group.getAgents().contains(agent2));
  }

  @Test
  void removeAgent_removesAgentFromGroup() {
    group.addAgent(agent1);
    group.addAgent(agent2);
    group.removeAgent(agent1);
    assertEquals(1, group.getAgents().size());
    assertFalse(group.getAgents().contains(agent1));
    assertTrue(group.getAgents().contains(agent2));
  }

  @Test
  void removeAgent_doesNothingWhenAgentNotInGroup() {
    group.addAgent(agent1);
    group.removeAgent(agent2);
    assertEquals(1, group.getAgents().size());
    assertTrue(group.getAgents().contains(agent1));
  }

  @Test
  void getAgents_returnsImmutableList() {
    group.addAgent(agent1);
    var agents = group.getAgents();
    assertEquals(1, agents.size());
  }

  @Test
  void equals_returnsTrueForSameIdAndName() {
    var group1 = new Group("Group A");
    var group2 = new Group("Group A");
    assertEquals(group1, group2);
  }

  @Test
  void equals_returnsFalseForDifferentName() {
    var group1 = new Group("Group A");
    var group2 = new Group("Group B");
    assertFalse(group1.equals(group2));
  }

  @Test
  void equals_returnsFalseForNull() {
    assertFalse(group.equals(null));
  }

  @Test
  void equals_returnsFalseForDifferentClass() {
    assertFalse(group.equals("Not a group"));
  }

  @Test
  void hashCode_isConsistentWithEquals() {
    var group1 = new Group("Group A");
    var group2 = new Group("Group A");
    assertEquals(group1.hashCode(), group2.hashCode());
  }
}
