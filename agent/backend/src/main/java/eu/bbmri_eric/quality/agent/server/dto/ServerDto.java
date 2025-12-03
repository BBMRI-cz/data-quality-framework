package eu.bbmri_eric.quality.agent.server.dto;

import eu.bbmri_eric.quality.agent.server.domain.ServerConnectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

/**
 * Data Transfer Object for basic Server information without interactions.
 *
 * <p>Contains basic server fields for list operations, excluding interactions and sensitive data to
 * improve performance and reduce response size.
 */
@Schema(description = "Basic server information")
@Relation(itemRelation = "server", collectionRelation = "servers")
public class ServerDto {

  /** Unique identifier for the server. */
  @Schema(description = "Unique identifier for the server", example = "1")
  private String id;

  /** URL of the central server. */
  @Schema(description = "URL of the central server", example = "https://central.example.com")
  private String url;

  /** Display name for the server. */
  @Schema(description = "Display name for the server", example = "Production Central Server")
  private String name;

  /** Current status of the server connection. */
  @Schema(description = "Current status of the server connection", example = "ACTIVE")
  private ServerConnectionStatus status;

  /** Default constructor. */
  public ServerDto() {}

  /**
   * Constructor with all fields.
   *
   * @param id the server ID
   * @param url the server URL
   * @param name the server name
   * @param status the server status
   */
  public ServerDto(String id, String url, String name, ServerConnectionStatus status) {
    this.id = id;
    this.url = url;
    this.name = name;
    this.status = status;
  }

  /**
   * Gets the ID.
   *
   * @return the ID
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the ID.
   *
   * @param id the ID
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the URL.
   *
   * @return the URL
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the URL.
   *
   * @param url the URL
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Gets the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the status.
   *
   * @return the status
   */
  public ServerConnectionStatus getStatus() {
    return status;
  }

  /**
   * Sets the status.
   *
   * @param status the status
   */
  public void setStatus(ServerConnectionStatus status) {
    this.status = status;
  }
}
