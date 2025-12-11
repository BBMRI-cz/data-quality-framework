package eu.bbmri_eric.quality.server.dataquality.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import eu.bbmri_eric.quality.server.dataquality.dto.CategoryDTO;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

/** HATEOAS link builder for Category resources. */
@Component
final class CategoryLinkBuilder {

  public EntityModel<CategoryDTO> toModel(CategoryDTO categoryDTO) {
    return EntityModel.of(categoryDTO)
        .add(linkTo(methodOn(CategoryController.class).findById(categoryDTO.getId())).withSelfRel())
        .add(linkTo(methodOn(CategoryController.class).findAll()).withRel("categories"));
  }

  public CollectionModel<EntityModel<CategoryDTO>> toCollectionModel(List<CategoryDTO> categories) {
    var entityModels = categories.stream().map(this::toModel).toList();

    return CollectionModel.of(entityModels)
        .add(linkTo(methodOn(CategoryController.class).findAll()).withSelfRel());
  }
}
