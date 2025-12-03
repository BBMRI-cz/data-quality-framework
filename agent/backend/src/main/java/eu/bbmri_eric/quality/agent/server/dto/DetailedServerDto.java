package eu.bbmri_eric.quality.agent.server.dto;

import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

/**
 * Detailed Data Transfer Object for returning complete Server information.
 *
 * <p>Extends ServerDto and adds sensitive fields and interactions for detailed view operations.
 */
@Setter
@Getter
@Schema(description = "Detailed server information including interactions and credentials")
@Relation(itemRelation = "server", collectionRelation = "servers")
public class DetailedServerDto extends ServerDto {

  @Schema(description = "Client ID used for authentication", example = "client-12345")
  private String clientId;

  @Schema(description = "List of interactions logged for this server")
  private List<ServerInteractionDto> interactions;

  /** Default constructor. */
  public DetailedServerDto() {
    super();
  }

  /**
   * Constructor with all fields.
   *
   * @param id the server ID
   * @param url the server URL
   * @param name the server name
   * @param status the server status
   * @param clientId the client ID
   * @param interactions the list of interactions
   */
  public DetailedServerDto(
      String id,
      String url,
      String name,
      ServerConnectionStatus status,
      String clientId,
      List<ServerInteractionDto> interactions) {
    super(id, url, name, status);
    this.clientId = clientId;
    this.interactions = interactions;
  }
}
