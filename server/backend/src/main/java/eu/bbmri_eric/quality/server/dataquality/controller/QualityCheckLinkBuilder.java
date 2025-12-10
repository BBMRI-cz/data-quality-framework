package eu.bbmri_eric.quality.server.dataquality.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDTO;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

/** HATEOAS link builder for QualityCheck resources. */
@Component
final class QualityCheckLinkBuilder {

  public EntityModel<QualityCheckDTO> toModel(QualityCheckDTO qualityCheckDto) {
    return EntityModel.of(qualityCheckDto)
        .add(
            linkTo(methodOn(QualityCheckController.class).findById(qualityCheckDto.getHash()))
                .withSelfRel())
        .add(linkTo(methodOn(QualityCheckController.class).findAll()).withRel("quality-checks"));
  }

  public CollectionModel<EntityModel<QualityCheckDTO>> toCollectionModel(
      List<QualityCheckDTO> qualityChecks) {
    var entityModels = qualityChecks.stream().map(this::toModel).toList();

    return CollectionModel.of(entityModels)
        .add(linkTo(methodOn(QualityCheckController.class).findAll()).withSelfRel());
  }
}
