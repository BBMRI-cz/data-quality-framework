package eu.bbmri_eric.quality.agent.server;

import eu.bbmri_eric.quality.agent.server.dto.RegistrationStateDto;

/**
 * Read-only view on the registration lifecycle state of this agent with a central server, derived
 * from the persisted registration fields of a {@link
 * eu.bbmri_eric.quality.agent.server.domain.Server}.
 */
public interface ServerRegistrationService {

  /**
   * Returns the registration state of the given server.
   *
   * @param serverId the ID of the server
   * @return the derived registration state
   * @throws jakarta.persistence.EntityNotFoundException if no server exists with the given ID
   */
  RegistrationStateDto findByServerId(String serverId);
}
