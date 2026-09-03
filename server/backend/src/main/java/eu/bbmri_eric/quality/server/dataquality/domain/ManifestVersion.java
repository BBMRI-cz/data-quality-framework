package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing an immutable, signed version of a manifest.
 *
 * <p>Each version belongs to exactly one {@link Manifest} and holds the JSON body of the manifest
 * snapshot together with its cryptographic signature and the id of the key that produced it.
 */
@Entity
@Table(name = "manifest_version")
public class ManifestVersion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "manifest_id", nullable = false)
  private Manifest manifest;

  @Column(name = "version", nullable = false)
  private int version;

  private final Instant generatedAt = Instant.now();

  /** JSON body of the manifest version. Stored as text because of serialization issues. */
  @Column(nullable = false, columnDefinition = "TEXT")
  @NotNull
  private String body;

  /** Cryptographic signature of the body. */
  @Column(columnDefinition = "TEXT")
  private String signature;

  /** Identifier of the key that produced the signature. */
  private String keyId;

  /** Default constructor for JPA. */
  protected ManifestVersion() {}

  /**
   * Creates a new manifest version. The generation timestamp defaults to the current time.
   *
   * @param manifest the manifest this version belongs to
   * @param version the version number
   * @param body the JSON body of the manifest version
   * @param signature the signature of the body, or null if not yet signed
   * @param keyId the id of the signing key, or null if not yet signed
   */
  public ManifestVersion(
      Manifest manifest, int version, String body, String signature, String keyId) {
    this.manifest = manifest;
    this.version = version;
    this.body = body;
    this.signature = signature;
    this.keyId = keyId;
  }

  /**
   * Gets the numeric id of this manifest version.
   *
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * Gets the manifest this version belongs to.
   *
   * @return the manifest
   */
  public Manifest getManifest() {
    return manifest;
  }

  /**
   * Sets the manifest this version belongs to. Only intended to be invoked by {@link Manifest} when
   * a version is added.
   *
   * @param manifest the manifest to set
   */
  void setManifest(Manifest manifest) {
    this.manifest = manifest;
  }

  /**
   * Gets the version number.
   *
   * @return the version number
   */
  public int getVersion() {
    return version;
  }

  /**
   * Gets the timestamp when this version was generated.
   *
   * @return the generation timestamp
   */
  public Instant getGeneratedAt() {
    return generatedAt;
  }

  /**
   * Gets the JSON body of this manifest version.
   *
   * @return the body
   */
  public String getBody() {
    return body;
  }

  /**
   * Sets the JSON body of this manifest version.
   *
   * @param body the body to set
   */
  public void setBody(String body) {
    this.body = body;
  }

  /**
   * Gets the signature of this version's body.
   *
   * @return the signature, or null if not yet signed
   */
  public String getSignature() {
    return signature;
  }

  /**
   * Sets the signature of this version's body.
   *
   * @param signature the signature to set
   */
  public void setSignature(String signature) {
    this.signature = signature;
  }

  /**
   * Gets the id of the key that produced the signature.
   *
   * @return the key id, or null if not yet signed
   */
  public String getKeyId() {
    return keyId;
  }

  /**
   * Sets the id of the key that produced the signature.
   *
   * @param keyId the key id to set
   */
  public void setKeyId(String keyId) {
    this.keyId = keyId;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ManifestVersion that = (ManifestVersion) o;
    return Objects.equals(manifest, that.manifest) && version == that.version;
  }

  @Override
  public int hashCode() {
    return Objects.hash(manifest, version);
  }
}
