package eu.bbmri_eric.quality.server.dataquality.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing a signed manifest of quality checks.
 *
 * <p>A manifest is an immutable, signed snapshot of the quality checks that agents should enforce.
 * The actual payload lives in the {@code body} column as JSON, together with the cryptographic
 * signature and the id of the key that produced it.
 */
@Entity
public class Manifest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private final Instant generatedAt = Instant.now();

  @NotNull private String name;

  /** JSON body of the manifest. Stored as text because of PostgreSQL serialization issues. */
  @Column(nullable = false, columnDefinition = "TEXT")
  @NotNull
  private String body;

  /** Cryptographic signature of the body. */
  @Column(columnDefinition = "TEXT")
  private String signature;

  /** Identifier of the key that produced the signature. */
  private String keyId;

  /** Default constructor for JPA. */
  protected Manifest() {}

  /**
   * Creates a new manifest. The generation timestamp defaults to the current time.
   *
   * @param name the name of the manifest
   * @param body the JSON body of the manifest
   * @param signature the signature of the body, or null if not yet signed
   * @param keyId the id of the signing key, or null if not yet signed
   */
  public Manifest(String name, String body, String signature, String keyId) {
    this.name = name;
    this.body = body;
    this.signature = signature;
    this.keyId = keyId;
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
   * Gets the timestamp when this manifest was generated.
   *
   * @return the generation timestamp
   */
  public Instant getGeneratedAt() {
    return generatedAt;
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
   * Gets the JSON body of this manifest.
   *
   * @return the body
   */
  public String getBody() {
    return body;
  }

  /**
   * Sets the JSON body of this manifest.
   *
   * @param body the body to set
   */
  public void setBody(String body) {
    this.body = body;
  }

  /**
   * Gets the signature of this manifest's body.
   *
   * @return the signature, or null if not yet signed
   */
  public String getSignature() {
    return signature;
  }

  /**
   * Sets the signature of this manifest's body.
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
    Manifest manifest = (Manifest) o;
    return Objects.equals(id, manifest.id)
        && Objects.equals(name, manifest.name)
        && Objects.equals(body, manifest.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, body);
  }
}
