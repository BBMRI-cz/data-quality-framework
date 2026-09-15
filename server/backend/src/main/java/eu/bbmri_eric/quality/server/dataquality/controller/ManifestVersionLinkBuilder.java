package eu.bbmri_eric.quality.server.dataquality.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import eu.bbmri_eric.quality.server.dataquality.dto.ManifestVersionDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDetailedDTO;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

/** HATEOAS link builder for ManifestVersion resources. */
@Component
final class ManifestVersionLinkBuilder {

  private final QualityCheckLinkBuilder qualityCheckLinkBuilder;

  public ManifestVersionLinkBuilder(QualityCheckLinkBuilder qualityCheckLinkBuilder) {
    this.qualityCheckLinkBuilder = qualityCheckLinkBuilder;
  }

  public EntityModel<ManifestVersionDTO> toModel(Long manifestId, ManifestVersionDTO versionDto) {
    return EntityModel.of(versionDto)
        .add(
            linkTo(methodOn(ManifestController.class).findVersions(manifestId))
                .withRel("manifest-versions"))
        .add(linkTo(methodOn(ManifestController.class).findById(manifestId)).withRel("manifest"))
        .add(
            linkTo(
                    methodOn(ManifestController.class)
                        .findVersionQualityChecks(manifestId, versionDto.getId()))
                .withRel("quality-checks"));
  }

  public CollectionModel<EntityModel<ManifestVersionDTO>> toCollectionModel(
      Long manifestId, List<ManifestVersionDTO> versions) {
    var entityModels = versions.stream().map(version -> toModel(manifestId, version)).toList();

    return CollectionModel.of(entityModels)
        .add(linkTo(methodOn(ManifestController.class).findVersions(manifestId)).withSelfRel());
  }

  public CollectionModel<EntityModel<QualityCheckDetailedDTO>> toQualityChecksCollectionModel(
      Long manifestId, Long versionId, List<QualityCheckDetailedDTO> qualityChecks) {
    var entityModels = qualityChecks.stream().map(qualityCheckLinkBuilder::toModel).toList();

    return CollectionModel.of(entityModels)
        .add(
            linkTo(
                    methodOn(ManifestController.class)
                        .findVersionQualityChecks(manifestId, versionId))
                .withSelfRel());
  }
}
