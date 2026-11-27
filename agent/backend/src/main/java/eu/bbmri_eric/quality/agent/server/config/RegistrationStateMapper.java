package eu.bbmri_eric.quality.agent.server.config;

import eu.bbmri_eric.quality.agent.server.domain.RegistrationStatus;
import eu.bbmri_eric.quality.agent.server.domain.Server;
import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import eu.bbmri_eric.quality.agent.server.dto.DetailedServerDto;
import eu.bbmri_eric.quality.agent.server.dto.RegistrationStateDto;
import jakarta.annotation.PostConstruct;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the ModelMapper type maps for the registration lifecycle view. This is the single place
 * mapping persisted state to {@link RegistrationStatus}: {@link RegistrationStatus#ACTIVE} is never
 * persisted and is derived here from an accepted registration combined with an {@link
 * ServerConnectionStatus#ACTIVE} server status. Mappings are declared explicitly because implicit
 * matching would wrongly pair {@link Server#getStatus()} (a {@link ServerConnectionStatus}) with
 * the derived registration status or match other {@code registration*} properties by name.
 */
@Configuration
public class RegistrationStateMapper {

  private final ModelMapper modelMapper;

  public RegistrationStateMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  /** Registers the type maps turning server state into its registration API view. */
  @PostConstruct
  public void addMappings() {
    Converter<Server, RegistrationStatus> deriveStatus =
        context -> {
          Server server = context.getSource();
          if (server.getRegistrationStatus() == RegistrationStatus.REGISTERED_PENDING_APPROVAL
              && server.getStatus() == ServerConnectionStatus.ACTIVE) {
            return RegistrationStatus.ACTIVE;
          }
          return server.getRegistrationStatus();
        };
    Converter<Server, Integer> attempts =
        context ->
            context.getSource().getRegistrationRetryCount() == null
                ? 0
                : context.getSource().getRegistrationRetryCount();

    modelMapper
        .emptyTypeMap(Server.class, RegistrationStateDto.class)
        .addMappings(
            mapping -> {
              mapping.using(deriveStatus).map(server -> server, RegistrationStateDto::setStatus);
              mapping.using(attempts).map(server -> server, RegistrationStateDto::setAttempts);
              mapping.map(
                  Server::getRegistrationErrorReason, RegistrationStateDto::setLastErrorReason);
              mapping.map(
                  Server::getRegistrationErrorMessage, RegistrationStateDto::setLastErrorMessage);
              mapping.map(
                  Server::getRegistrationRegisteredAt, RegistrationStateDto::setRegisteredAt);
              mapping.map(Server::getNextRegistrationRetryAt, RegistrationStateDto::setNextRetryAt);
            });

    modelMapper
        .emptyTypeMap(Server.class, DetailedServerDto.class)
        .addMappings(
            mapping ->
                mapping
                    .using(
                        (Converter<Server, RegistrationStateDto>)
                            context ->
                                modelMapper.map(context.getSource(), RegistrationStateDto.class))
                    .map(server -> server, DetailedServerDto::setRegistration))
        .implicitMappings();
  }
}
