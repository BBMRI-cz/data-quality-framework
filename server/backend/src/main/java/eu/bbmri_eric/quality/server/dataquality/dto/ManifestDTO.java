package eu.bbmri_eric.quality.server.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.springframework.hateoas.server.core.Relation;

/** DTO for manifest data. */
@Schema(name = "Manifest", description = "A signed manifest of quality checks")
@Relation(itemRelation = "manifest", collectionRelation = "manifests")
public class ManifestDTO {

  @Schema(
      description = "Numeric id of the manifest",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  @Schema(
      description = "Name of the manifest",
      example = "Quality Checks 2026-08",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(
      description = "When this manifest was generated",
      example = "2026-08-13T10:00:00Z",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Instant generatedAt;

  @Schema(
      description = "JSON body of the manifest",
      example = "{\"manifest_version\":1042,\"checks\":[]}",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String body;

  @Schema(description = "Cryptographic signature of the body", example = "a1b2c3d4e5f6...")
  private String signature;

  @Schema(
      description = "Identifier of the key that produced the signature",
      example = "central-server-key")
  private String keyId;

  /** Default constructor for serialization frameworks. */
  public ManifestDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param name the name of the manifest
   * @param generatedAt when this manifest was generated
   * @param body the JSON body of the manifest
   * @param signature the signature of the body
   * @param keyId the id of the signing key
   */
  public ManifestDTO(
      String name, Instant generatedAt, String body, String signature, String keyId) {
    this.name = name;
    this.generatedAt = generatedAt;
    this.body = body;
    this.signature = signature;
    this.keyId = keyId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(Instant generatedAt) {
    this.generatedAt = generatedAt;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public String getKeyId() {
    return keyId;
  }

  public void setKeyId(String keyId) {
    this.keyId = keyId;
  }
}
