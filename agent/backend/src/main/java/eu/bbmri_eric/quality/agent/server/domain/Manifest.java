package eu.bbmri_eric.quality.agent.server.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A quality check manifest published by a registered central server.
 *
 * <p>The entity stores the local state of a manifest the agent has seen or downloaded. The
 * reference to the originating {@link Server} together with the remote identifier makes every
 * manifest unique per server. {@code installedVersion} stays {@code null} until a manifest version
 * is actually downloaded and its checks are installed locally.
 *
 * <p>The manifest tracks which local quality checks were installed from it by their IDs, as the
 * {@code QualityCheck} entity lives in the {@code dataquality} module and is not exposed across
 * module boundaries.
 */
@Entity
public class Manifest {

  @Id private String id = UUID.randomUUID().toString();

  @NotNull private Long remoteId;

  @NotBlank
  @Size(max = 255)
  private String name;

  private Integer installedVersion;

  @NotNull
  @ManyToOne(optional = false)
  private Server server;

  /** IDs of the local {@code QualityCheck} entities installed from this manifest. */
  @ElementCollection
  @CollectionTable(name = "manifest_quality_check", joinColumns = @JoinColumn(name = "manifest_id"))
  @Column(name = "quality_check_id")
  private final Set<Long> qualityCheckIds = new LinkedHashSet<>();

  protected Manifest() {}

  public Manifest(Long remoteId, String name, Server server) {
    this.remoteId = remoteId;
    this.name = name;
    this.server = server;
  }

  public String getId() {
    return id;
  }

  public Long getRemoteId() {
    return remoteId;
  }

  public void setRemoteId(Long remoteId) {
    this.remoteId = remoteId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getInstalledVersion() {
    return installedVersion;
  }

  public void setInstalledVersion(Integer installedVersion) {
    this.installedVersion = installedVersion;
  }

  public Server getServer() {
    return server;
  }

  public void setServer(Server server) {
    this.server = server;
  }

  public Set<Long> getQualityCheckIds() {
    return qualityCheckIds;
  }

  public void addQualityCheckId(Long qualityCheckId) {
    qualityCheckIds.add(qualityCheckId);
  }

  public void removeQualityCheckId(Long qualityCheckId) {
    qualityCheckIds.remove(qualityCheckId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Manifest manifest = (Manifest) o;
    return Objects.equals(id, manifest.id)
        && Objects.equals(remoteId, manifest.remoteId)
        && Objects.equals(name, manifest.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, remoteId, name);
  }

  @Override
  public String toString() {
    return "Manifest{"
        + "id="
        + id
        + ", remoteId="
        + remoteId
        + ", name='"
        + name
        + '\''
        + ", installedVersion="
        + installedVersion
        + '}';
  }
}
