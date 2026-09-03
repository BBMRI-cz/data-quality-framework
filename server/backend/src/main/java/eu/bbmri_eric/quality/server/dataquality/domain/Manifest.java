package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entity representing a manifest of quality checks.
 *
 * <p>A manifest holds the manifest's metadata (currently only its name) and owns a set of
 * immutable, signed {@link ManifestVersion}s. Publishing a new release appends a new version.
 */
@Entity
public class Manifest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull private String name;

  @OneToMany(mappedBy = "manifest", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("version ASC")
  private final Set<ManifestVersion> versions = new LinkedHashSet<>();

  /** Default constructor for JPA. */
  protected Manifest() {}

  /**
   * Creates a new manifest with the given name.
   *
   * @param name the name of the manifest
   */
  public Manifest(String name) {
    this.name = name;
  }

  /**
   * Gets the numeric id of this manifest.
   *
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * Gets the name of this manifest.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of this manifest.
   *
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the versions of this manifest (lazy-loaded).
   *
   * @return the set of versions
   */
  public Set<ManifestVersion> getVersions() {
    return versions;
  }

  /**
   * Adds a new version to this manifest, establishing the back-reference from the version.
   *
   * @param version the version to add
   */
  public void addVersion(ManifestVersion version) {
    version.setManifest(this);
    versions.add(version);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Manifest manifest = (Manifest) o;
    return Objects.equals(id, manifest.id) && Objects.equals(name, manifest.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }
}
