package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "Agent Registration Request")
public class AgentRegistrationRequest {

  @NotBlank(message = "Agent ID cannot be blank")
  @Pattern(
      regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
      message = "Agent ID must be a valid UUID format")
  private String id;

  @Size(max = 50, message = "Version must not exceed 50 characters")
  private String version = "unknown";

  protected AgentRegistrationRequest() {
    // for model mapper
  }

  public AgentRegistrationRequest(String id) {
    this.id = id;
  }

  public AgentRegistrationRequest(String id, String version) {
    this.id = id;
    this.version = version;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
