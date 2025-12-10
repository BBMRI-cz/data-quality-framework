package eu.bbmri_eric.quality.server.dataquality.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgentTest {

  @Test
  void constructor_shouldCreateAgentWithIdAndRegistrationInteraction() {
    String agentId = UUID.randomUUID().toString();

    Agent agent = new Agent(agentId);

    assertEquals(agentId, agent.getId());
    assertEquals(AgentStatus.PENDING, agent.getStatus());
    assertEquals("Unknown", agent.getVersion());
    assertNull(agent.getName());
    assertEquals(1, agent.getInteractions().size());
    assertEquals(AgentInteractionType.REGISTRATION, agent.getInteractions().get(0).getType());
  }

  @Test
  void setStatus_shouldUpdateAgentStatus() {
    Agent agent = new Agent(UUID.randomUUID().toString());

    agent.setStatus(AgentStatus.ACTIVE);

    assertEquals(AgentStatus.ACTIVE, agent.getStatus());
  }

  @Test
  void setName_shouldUpdateAgentName() {
    Agent agent = new Agent(UUID.randomUUID().toString());

    agent.setName("Test Agent");

    assertEquals("Test Agent", agent.getName());
  }

  @Test
  void setVersion_shouldUpdateVersionAndAddInteraction() {
    Agent agent = new Agent(UUID.randomUUID().toString());
    int initialInteractionsCount = agent.getInteractions().size();

    agent.setVersion("1.0.0");

    assertEquals("1.0.0", agent.getVersion());
    assertEquals(initialInteractionsCount + 1, agent.getInteractions().size());
    assertEquals(
        AgentInteractionType.VERSION_UPDATE,
        agent.getInteractions().get(0).getType()); // Most recent first
  }

  @Test
  void addInteraction_shouldAddNewInteraction() {
    Agent agent = new Agent(UUID.randomUUID().toString());
    int initialCount = agent.getInteractions().size();

    agent.addInteraction(AgentInteractionType.PING);

    assertEquals(initialCount + 1, agent.getInteractions().size());
    assertEquals(
        AgentInteractionType.PING, agent.getInteractions().get(0).getType()); // Most recent first
  }

  @Test
  void addInteraction_shouldThrowExceptionWhenTypeIsNull() {
    Agent agent = new Agent(UUID.randomUUID().toString());

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> agent.addInteraction(null));

    assertEquals("Interaction type cannot be null", exception.getMessage());
  }

  @Test
  void getInteractions_shouldReturnInteractionsSortedByTimestampDescending() {
    Agent agent = new Agent(UUID.randomUUID().toString());

    agent.addInteraction(AgentInteractionType.PING);
    agent.addInteraction(AgentInteractionType.REPORT);

    List<AgentInteraction> interactions = agent.getInteractions();

    assertEquals(3, interactions.size()); // REGISTRATION + PING + REPORT
    assertEquals(AgentInteractionType.REPORT, interactions.get(0).getType());
    assertEquals(AgentInteractionType.PING, interactions.get(1).getType());
    assertEquals(AgentInteractionType.REGISTRATION, interactions.get(2).getType());
  }

  @Test
  void addReport_shouldAddReportAndSetAgentId() {
    String agentId = UUID.randomUUID().toString();
    Agent agent = new Agent(agentId);
    Report report = new Report();

    agent.addReport(report);

    assertEquals(1, agent.getReports().size());
    assertEquals(agentId, report.getAgentId());
    assertTrue(agent.getReports().contains(report));
  }

  @Test
  void getReports_shouldReturnAllReports() {
    Agent agent = new Agent(UUID.randomUUID().toString());
    Report report1 = new Report();
    Report report2 = new Report();

    agent.addReport(report1);
    agent.addReport(report2);

    List<Report> reports = agent.getReports();

    assertEquals(2, reports.size());
    assertTrue(reports.contains(report1));
    assertTrue(reports.contains(report2));
  }

  @Test
  void equals_shouldReturnTrueForSameAgentInstance() {
    Agent agent = new Agent(UUID.randomUUID().toString());

    assertEquals(agent, agent);
  }

  @Test
  void equals_shouldReturnFalseForDifferentAgentsWithDifferentIds() {
    Agent agent1 = new Agent(UUID.randomUUID().toString());
    Agent agent2 = new Agent(UUID.randomUUID().toString());

    assertNotEquals(agent1, agent2);
  }

  @Test
  void equals_shouldReturnFalseForAgentsWithSameIdButDifferentInteractions() {
    String agentId = UUID.randomUUID().toString();
    Agent agent1 = new Agent(agentId);
    Agent agent2 = new Agent(agentId);

    // Even with the same ID, agents have different timestamps for their REGISTRATION interaction
    assertNotEquals(agent1, agent2);
  }

  @Test
  void equals_shouldReturnFalseForNull() {
    Agent agent = new Agent(UUID.randomUUID().toString());

    assertNotEquals(null, agent);
  }

  @Test
  void equals_shouldReturnFalseForDifferentClass() {
    Agent agent = new Agent(UUID.randomUUID().toString());

    assertNotEquals(new Object(), agent);
  }

  @Test
  void hashCode_shouldBeConsistentForSameInstance() {
    Agent agent = new Agent(UUID.randomUUID().toString());

    assertEquals(agent.hashCode(), agent.hashCode());
  }

  @Test
  void hashCode_shouldBeDifferentForDifferentAgents() {
    Agent agent1 = new Agent(UUID.randomUUID().toString());
    Agent agent2 = new Agent(UUID.randomUUID().toString());

    assertNotEquals(agent1.hashCode(), agent2.hashCode());
  }
}
