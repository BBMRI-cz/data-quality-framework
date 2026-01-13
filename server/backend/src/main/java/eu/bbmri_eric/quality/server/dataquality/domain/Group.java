package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing a group of agents.
 *
 * <p>Groups allow organizing agents into logical collections. Each agent can belong to multiple
 * groups, and each group can contain multiple agents.
 */
@Entity
@Table(name = "agent_group")
public class Group {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(unique = true)
  private String name;

  @ManyToMany
  @JoinTable(
      name = "group_agent",
      joinColumns = @JoinColumn(name = "group_id"),
      inverseJoinColumns = @JoinColumn(name = "agent_id"))
  private final List<Agent> agents = new ArrayList<>();

  /** Default constructor for JPA. */
  protected Group() {}

  /**
   * Creates a new group.
   *
   * @param name the unique name of the group
   */
  public Group(String name) {
    this.name = name;
  }

  /**
   * Gets the unique identifier of this group.
   *
   * @return the group ID
   */
  public Long getId() {
    return id;
  }

  /**
   * Gets the name of this group.
   *
   * @return the group name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of this group.
   *
   * @param name the group name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the list of agents in this group.
   *
   * @return immutable list of agents
   */
  public List<Agent> getAgents() {
    return agents.stream().toList();
  }

  /**
   * Adds an agent to this group.
   *
   * @param agent the agent to add
   * @throws IllegalArgumentException if the agent is null
   */
  public void addAgent(Agent agent) {
    if (agent == null) {
      throw new IllegalArgumentException("Agent cannot be null");
    }
    if (!agents.contains(agent)) {
      agents.add(agent);
    }
  }

  /**
   * Removes an agent from this group.
   *
   * @param agent the agent to remove
   */
  public void removeAgent(Agent agent) {
    agents.remove(agent);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Group group = (Group) o;
    return Objects.equals(id, group.id) && Objects.equals(name, group.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }
}
