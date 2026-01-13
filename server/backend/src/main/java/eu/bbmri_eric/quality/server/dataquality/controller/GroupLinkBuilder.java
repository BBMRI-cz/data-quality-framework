package eu.bbmri_eric.quality.server.dataquality.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import eu.bbmri_eric.quality.server.dataquality.dto.GroupDTO;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

@Component
final class GroupLinkBuilder {

  public EntityModel<GroupDTO> toModel(GroupDTO groupDTO) {
    return EntityModel.of(groupDTO)
        .add(linkTo(methodOn(GroupController.class).findById(groupDTO.getId())).withSelfRel())
        .add(linkTo(methodOn(GroupController.class).findAll()).withRel("groups"));
  }

  public CollectionModel<EntityModel<GroupDTO>> toCollectionModel(List<GroupDTO> groups) {
    var entityModels = groups.stream().map(this::toModel).toList();

    return CollectionModel.of(entityModels)
        .add(linkTo(methodOn(GroupController.class).findAll()).withSelfRel());
  }
}
