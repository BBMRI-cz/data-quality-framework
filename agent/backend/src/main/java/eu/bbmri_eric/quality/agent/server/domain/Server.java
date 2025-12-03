package eu.bbmri_eric.quality.agent.server.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
public class Server {
  @Id private String id = UUID.randomUUID().toString();

  @NotBlank
  @Size(max = 500)
  @Column(name = "url", nullable = false, length = 500, unique = true)
  private String url;

  @NotBlank
  @Size(max = 255)
  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Size(max = 255)
  @Column(name = "client_id", nullable = false, length = 255)
  private String clientId = "";

  @Size(max = 500)
  @Column(name = "client_secret", nullable = false, length = 500)
  private String clientSecret = "";

  @NotNull
  @Enumerated(EnumType.STRING)
  private ServerConnectionStatus status = ServerConnectionStatus.PENDING;

  @OneToMany(mappedBy = "serverId", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ServerInteraction> interactions = new ArrayList<>();

  protected Server() {}

  public Server(
      String url,
      String name,
      String clientId,
      String clientSecret,
      ServerConnectionStatus status) {
    this.url = url;
    this.name = name;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.status = status;
    addInteraction(
        new ServerInteraction(InteractionType.UPDATE, "Central server added to the database"));
  }

  public Server(String url, String name) {
    this.url = url;
    this.name = name;
    addInteraction(
        new ServerInteraction(InteractionType.UPDATE, "Central server added to the database"));
  }

  public String getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    String oldUrl = this.url;
    this.url = url;
    if (this.id != null && !Objects.equals(oldUrl, url)) {
      addInteraction(
          new ServerInteraction(
              InteractionType.UPDATE, String.format("URL updated from '%s' to '%s'", oldUrl, url)));
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    String oldName = this.name;
    this.name = name;
    if (this.id != null && !Objects.equals(oldName, name)) {
      addInteraction(
          new ServerInteraction(
              InteractionType.UPDATE,
              String.format("Name updated from '%s' to '%s'", oldName, name)));
    }
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    String oldClientId = this.clientId;
    this.clientId = clientId;
    if (this.id != null && !Objects.equals(oldClientId, clientId)) {
      addInteraction(
          new ServerInteraction(
              InteractionType.UPDATE,
              String.format("Client ID updated from '%s' to '%s'", oldClientId, clientId)));
    }
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    boolean hadSecret = this.clientSecret != null && !this.clientSecret.isEmpty();
    this.clientSecret = clientSecret;
    if (this.id != null && hadSecret) {
      addInteraction(new ServerInteraction(InteractionType.UPDATE, "Client secret updated"));
    }
  }

  public ServerConnectionStatus getStatus() {
    return status;
  }

  public void setStatus(ServerConnectionStatus status) {
    ServerConnectionStatus oldStatus = this.status;
    this.status = status;
    if (this.id != null && oldStatus != status) {
      addInteraction(
          new ServerInteraction(
              InteractionType.UPDATE,
              String.format("Status changed from %s to %s", oldStatus, status)));
    }
  }

  public List<ServerInteraction> getInteractions() {
    return Collections.unmodifiableList(interactions);
  }

  public void addInteraction(ServerInteraction interaction) {
    interaction.setServerId(this.id);
    this.interactions.add(interaction);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Server server = (Server) o;
    return Objects.equals(id, server.id)
        && Objects.equals(url, server.url)
        && Objects.equals(name, server.name)
        && Objects.equals(clientId, server.clientId)
        && Objects.equals(clientSecret, server.clientSecret)
        && status == server.status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, url, name, clientId, clientSecret, status);
  }

  @Override
  public String toString() {
    return "Server{"
        + "id="
        + id
        + ", url='"
        + url
        + '\''
        + ", name='"
        + name
        + '\''
        + ", clientId='"
        + clientId
        + '\''
        + ", status="
        + status
        + '}';
  }
}
