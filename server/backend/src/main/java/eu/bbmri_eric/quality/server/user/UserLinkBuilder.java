package eu.bbmri_eric.quality.server.user;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Component;

@Component
public class UserLinkBuilder {

  public EntityModel<UserDTO> toModel(UserDTO user) {
    var model = EntityModel.of(user);

    // Self link pointing to userinfo as that's the main way to get "me"
    model.add(linkTo(methodOn(UserController.class).getCurrentUser(null)).withSelfRel());

    // Link to change password
    model.add(
        linkTo(methodOn(UserController.class).changePassword(user.getId(), null))
            .withRel("change-password"));

    return model;
  }

  public CollectionModel<EntityModel<UserDTO>> toCollectionModel(List<UserDTO> users) {
    var entityModels = users.stream().map(this::toModel).toList();

    return CollectionModel.of(entityModels)
        .add(linkTo(methodOn(UserController.class).findAll()).withSelfRel());
  }
}
