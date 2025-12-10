package eu.bbmri_eric.quality.server.dataquality;

import eu.bbmri_eric.quality.server.dataquality.dto.AgentDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistration;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentRegistrationRequest;
import eu.bbmri_eric.quality.server.dataquality.dto.AgentUpdateRequest;
import java.util.List;

/** Service interface for managing agents and their lifecycle. */
public interface AgentService {
  /**
   * Creates a new agent with associated user credentials.
   *
   * @param createAgentDto the agent registration request containing agent details
   * @return the created agent registration with agent and user information
   */
  AgentRegistration create(AgentRegistrationRequest createAgentDto);

  /**
   * Updates an existing agent's information.
   *
   * @param updateAgentDto the agent update request containing updated fields
   * @param agentId the unique identifier of the agent to update
   * @return the updated agent data
   */
  AgentDTO update(AgentUpdateRequest updateAgentDto, String agentId);

  /**
   * Finds an agent by its unique identifier.
   *
   * @param id the unique identifier of the agent
   * @return the agent data
   */
  AgentDTO findById(String id);

  /**
   * Finds an agent by its unique identifier with optional interaction expansion.
   *
   * @param id the unique identifier of the agent
   * @param expandInteractions if true, includes the agent's interactions in the response
   * @return the agent data with or without interactions
   */
  AgentDTO findById(String id, boolean expandInteractions);

  /**
   * Retrieves all registered agents.
   *
   * @return list of all agents
   */
  List<AgentDTO> listAll();

  /**
   * Counts the total number of registered agents.
   *
   * @return the total count of agents
   */
  long countAll();

  /**
   * Deletes an agent by its unique identifier.
   *
   * @param id the unique identifier of the agent to delete
   */
  void delete(String id);
}
