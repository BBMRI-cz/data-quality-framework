package eu.bbmri_eric.quality.server.dataquality;

import eu.bbmri_eric.quality.server.common.CRUDService;
import eu.bbmri_eric.quality.server.dataquality.dto.GroupCreateDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.GroupDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.GroupUpdateDTO;
import java.util.List;

/** Service interface for managing groups. */
public interface GroupService extends CRUDService<GroupDTO, GroupCreateDTO, GroupUpdateDTO, Long> {

  /**
   * Assigns a list of agents to a group.
   *
   * @param groupId the ID of the group
   * @param agentIds the list of agent IDs to assign to the group
   * @return the updated group with assigned agents
   */
  GroupDTO assignAgents(Long groupId, List<String> agentIds);
}
