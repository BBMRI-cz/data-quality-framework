package eu.bbmri_eric.quality.agent.server;

import eu.bbmri_eric.quality.agent.server.dto.DetailedServerDto;
import eu.bbmri_eric.quality.agent.server.dto.ServerCreateDto;
import eu.bbmri_eric.quality.agent.server.dto.ServerDto;
import eu.bbmri_eric.quality.agent.server.dto.ServerUpdateDto;
import java.util.List;

public interface ServerService {

  List<ServerDto> findAll();

  DetailedServerDto findById(String id);

  ServerDto create(ServerCreateDto createDto);

  ServerDto update(String id, ServerUpdateDto updateDto);

  void deleteById(String id);

  /**
   * Re-triggers the registration of a server with the central server: resets the automatic retry
   * counters and publishes a new registration event. The outcome is asynchronous and shows up in
   * the server's interactions; a concurrent in-flight registration attempt may overwrite the reset
   * state with its own outcome.
   *
   * @param id the ID of the server to register again
   */
  void retryRegistration(String id);
}
