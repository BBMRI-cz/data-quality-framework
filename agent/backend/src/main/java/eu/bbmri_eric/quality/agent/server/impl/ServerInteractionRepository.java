package eu.bbmri_eric.quality.agent.server.impl;

import eu.bbmri_eric.quality.agent.server.domain.ServerInteraction;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/** Repository for persisting central server interactions independently of server configuration. */
@Repository
interface ServerInteractionRepository extends CrudRepository<ServerInteraction, Long> {}
