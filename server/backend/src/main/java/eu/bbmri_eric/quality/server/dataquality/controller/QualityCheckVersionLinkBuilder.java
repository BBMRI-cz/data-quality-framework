package eu.bbmri_eric.quality.server.dataquality.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckVersionDTO;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

/** HATEOAS link builder for QualityCheckVersion resources. */
@Component
final class QualityCheckVersionLinkBuilder {

  public EntityModel<QualityCheckVersionDTO> toModel(
      Long qualityCheckId, QualityCheckVersionDTO versionDto) {
    return EntityModel.of(versionDto)
        .add(
            linkTo(methodOn(QualityCheckController.class).findVersions(qualityCheckId))
                .withRel("quality-check-versions"))
        .add(
            linkTo(methodOn(QualityCheckController.class).findById(qualityCheckId))
                .withRel("quality-check"));
  }

  public CollectionModel<EntityModel<QualityCheckVersionDTO>> toCollectionModel(
      Long qualityCheckId, List<QualityCheckVersionDTO> versions) {
    var entityModels = versions.stream().map(version -> toModel(qualityCheckId, version)).toList();

    return CollectionModel.of(entityModels)
        .add(
            linkTo(methodOn(QualityCheckController.class).findVersions(qualityCheckId))
                .withSelfRel());
  }
}
