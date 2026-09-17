package eu.bbmri_eric.quality.agent.server.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** A single signed version of a quality check manifest published by a central server. */
@Schema(name = "Manifest Version", description = "A signed version of a quality check manifest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManifestVersionDto {

  @Schema(description = "ID of the manifest version on the central server", example = "42")
  @JsonAlias("id")
  private Long remoteId;

  @Schema(description = "Version number of the manifest", example = "1")
  private int version;

  @Schema(description = "When this version was generated", example = "2026-08-13T10:00:00Z")
  private Instant generatedAt;

  @Schema(
      description =
          "Signed JSON body of the manifest version listing the quality checks it pins down")
  private JsonNode body;

  @Schema(description = "Cryptographic signature of the body", example = "MEUCIBd...")
  private String signature;

  @Schema(
      description = "Identifier of the key that produced the signature",
      example = "central-signing")
  private String keyId;

  /** Default constructor for serialization frameworks. */
  public ManifestVersionDto() {}

  public ManifestVersionDto(
      Long remoteId,
      int version,
      Instant generatedAt,
      JsonNode body,
      String signature,
      String keyId) {
    this.remoteId = remoteId;
    this.version = version;
    this.generatedAt = generatedAt;
    this.body = body;
    this.signature = signature;
    this.keyId = keyId;
  }

  public Long getRemoteId() {
    return remoteId;
  }

  public void setRemoteId(Long remoteId) {
    this.remoteId = remoteId;
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

  public JsonNode getBody() {
    return body;
  }

  public void setBody(JsonNode body) {
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
