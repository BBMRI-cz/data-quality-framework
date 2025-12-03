package eu.bbmri_eric.quality.agent.dataquality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.hateoas.server.core.Relation;

/** DTO for quality check result data. */
@Schema(name = "Quality Check Result", description = "Result of a quality check execution")
@Relation(itemRelation = "result", collectionRelation = "results")
public record QualityCheckResultDTO(
    @NotBlank(message = "Quality check hash cannot be blank")
        @Size(min = 1, max = 255, message = "Hash must be between 1 and 255 characters")
        @Pattern(
            regexp = "^[a-zA-Z0-9_-]+$",
            message = "Hash must contain only alphanumeric characters, underscores, and hyphens")
        @Schema(
            description = "Hash identifying the quality check",
            example = "abc123def456",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String hash,
    @NotNull(message = "Result cannot be null")
        @Schema(
            description = "Numeric result of the quality check",
            example = "0.95",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Double result) {}
