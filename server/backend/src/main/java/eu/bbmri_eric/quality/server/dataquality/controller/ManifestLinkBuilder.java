package eu.bbmri_eric.quality.server.dataquality.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import eu.bbmri_eric.quality.server.dataquality.dto.ManifestDTO;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

/** HATEOAS link builder for Manifest resources. */
@Component
final class ManifestLinkBuilder {

  public EntityModel<ManifestDTO> toModel(ManifestDTO manifestDTO) {
    return EntityModel.of(manifestDTO)
        .add(linkTo(methodOn(ManifestController.class).findById(manifestDTO.getId())).withSelfRel())
        .add(linkTo(methodOn(ManifestController.class).findAll()).withRel("manifests"));
  }

  public CollectionModel<EntityModel<ManifestDTO>> toCollectionModel(List<ManifestDTO> manifests) {
    var entityModels = manifests.stream().map(this::toModel).toList();

    return CollectionModel.of(entityModels)
        .add(linkTo(methodOn(ManifestController.class).findAll()).withSelfRel());
  }
}
