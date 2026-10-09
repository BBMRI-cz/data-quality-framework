package eu.bbmri_eric.quality.server.user.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.quality.server.common.LinkBuilder;
import eu.bbmri_eric.quality.server.common.dto.PageResponse;
import eu.bbmri_eric.quality.server.user.dto.UserDTO;
import eu.bbmri_eric.quality.server.user.dto.UserFilterDTO;
import java.util.List;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
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

  public PagedModel<EntityModel<UserDTO>> toPagedModel(
      PageResponse<UserDTO> pageResponse, UserFilterDTO filter) {
    List<EntityModel<UserDTO>> userModels =
        pageResponse.getContent().stream().map(this::toModel).toList();

    PagedModel.PageMetadata metadata =
        new PagedModel.PageMetadata(
            pageResponse.getSize(),
            pageResponse.getPage(),
            pageResponse.getTotalElements(),
            pageResponse.getTotalPages());

    PagedModel<EntityModel<UserDTO>> pagedModel = PagedModel.of(userModels, metadata);
    Link selfLink = linkTo(methodOn(UserController.class).findAll(filter)).withSelfRel();

    pagedModel.add(selfLink);
    List<Link> paginationLinks = LinkBuilder.getPageLinks(selfLink.toUri(), filter, pageResponse);
    pagedModel.add(paginationLinks);

    return pagedModel;
  }
}
