package eu.bbmri_eric.quality.agent.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.util.HtmlUtils;

/**
 * Data Transfer Object for updating an existing Server.
 *
 * <p>Contains only the fields that can be updated: name, URL, clientId, and clientSecret. All
 * fields are optional to allow partial updates.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data for updating an existing server (all fields optional)")
public class ServerUpdateDto {

  /** URL of the central server. */
  @Size(max = 500, message = "URL must not exceed 500 characters")
  @Schema(description = "URL of the central server", example = "https://central.example.com")
  @Pattern(
      regexp = "^https?://[^\\s/$.?#].[^\\s]*$",
      message = "URL must be a valid HTTP or HTTPS URL")
  private String url;

  /** Display name for the server. */
  @Size(max = 255, message = "Name must not exceed 255 characters")
  @Schema(description = "Display name for the server", example = "Production Central Server")
  private String name;

  /** Client ID used for authentication with the server. */
  @Size(max = 255, message = "Client ID must not exceed 255 characters")
  @Schema(description = "Client ID used for authentication", example = "client-12345")
  @Setter
  private String clientId;

  /** Client secret used for authentication with the server. */
  @Size(max = 500, message = "Client secret must not exceed 500 characters")
  @Schema(
      description = "Client secret used for authentication (Base64 encoded)",
      example = "Y2xpZW50LXNlY3JldA==")
  @Setter
  private String clientSecret;

  /**
   * Sets the URL.
   *
   * @param url the URL
   */
  public void setUrl(String url) {
    this.url = url != null ? url.trim() : null;
  }

  /**
   * Sets the name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name != null ? HtmlUtils.htmlEscape(name.trim()) : null;
  }
}
