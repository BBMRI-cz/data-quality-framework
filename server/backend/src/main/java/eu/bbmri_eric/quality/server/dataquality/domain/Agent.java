package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
public class Agent {
  @Id private String id;

  @Enumerated(EnumType.STRING)
  private AgentStatus status = AgentStatus.PENDING;

  private String name;

  private String version = "Unknown";

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "agent_id", nullable = false)
  private List<AgentInteraction> interactions = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "agent_id", nullable = false)
  private List<Report> reports = new ArrayList<>();

  public Agent(String id) {
    this.id = id;
    addInteraction(AgentInteractionType.REGISTRATION);
  }

  protected Agent() {}

  public String getId() {
    return id;
  }

  public AgentStatus getStatus() {
    return status;
  }

  public void setStatus(AgentStatus status) {
    this.status = status;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
    addInteraction(AgentInteractionType.VERSION_UPDATE);
  }

  public List<AgentInteraction> getInteractions() {
    return interactions.stream()
        .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
        .toList();
  }

  public void addInteraction(AgentInteractionType type) {
    if (type == null) {
      throw new IllegalArgumentException("Interaction type cannot be null");
    }
    AgentInteraction interaction = new AgentInteraction(this.id, type);
    interactions.add(interaction);
  }

  public List<Report> getReports() {
    return reports.stream().toList();
  }

  public void addReport(Report report) {
    report.setAgentId(getId());
    reports.add(report);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Agent agent = (Agent) o;
    return Objects.equals(id, agent.id)
        && status == agent.status
        && Objects.equals(name, agent.name)
        && Objects.equals(version, agent.version)
        && Objects.equals(interactions, agent.interactions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, status, name, version, interactions);
  }
}
