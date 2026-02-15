package eu.bbmri_eric.quality.agent.server;

import eu.bbmri_eric.quality.agent.dataquality.dto.ObfuscatedReportDTO;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;

public interface CentralServerClient {
  RegistrationCredentials register() throws Exception;

  ServerConnectionStatus checkRegistrationStatus() throws Exception;

  void healthCheck();

  void updateAgentVersion(String version) throws Exception;

  void sendReport(ObfuscatedReportDTO reportDTO) throws Exception;
}
