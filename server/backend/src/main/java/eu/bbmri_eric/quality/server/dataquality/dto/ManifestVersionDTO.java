package eu.bbmri_eric.quality.server.dataquality.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.springframework.hateoas.server.core.Relation;

/** DTO for a single, signed manifest version. */
@Schema(name = "Manifest Version", description = "A signed version of a quality check manifest")
@Relation(itemRelation = "manifestVersion", collectionRelation = "manifestVersions")
public class ManifestVersionDTO {

  @Schema(
      description = "Numeric id of the manifest version",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  @Schema(
      description = "Version number of the manifest",
      example = "1",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private int version;

  @Schema(
      description = "When this version was generated",
      example = "2026-08-13T10:00:00Z",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Instant generatedAt;

  @Schema(
      description = "JSON body of the manifest version",
      example = "{\"manifest_id\":1,\"generated_at\":\"2026-08-13T10:00:00Z\",\"checks\":[]}",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String body;

  @Schema(description = "Cryptographic signature of the body", example = "a1b2c3d4e5f6...")
  private String signature;

  @Schema(
      description = "Identifier of the key that produced the signature",
      example = "central-server-key")
  private String keyId;

  /** Default constructor for serialization frameworks. */
  public ManifestVersionDTO() {}

  /**
   * Constructor with all fields.
   *
   * @param version the version number
   * @param generatedAt when this version was generated
   * @param body the JSON body of the manifest version
   * @param signature the signature of the body
   * @param keyId the id of the signing key
   */
  public ManifestVersionDTO(
      int version, Instant generatedAt, String body, String signature, String keyId) {
    this.version = version;
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

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(Instant generatedAt) {
    this.generatedAt = generatedAt;
  }

  @JsonRawValue
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
