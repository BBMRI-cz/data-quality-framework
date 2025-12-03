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
}
