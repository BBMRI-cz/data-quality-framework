package eu.bbmri_eric.quality.server.dataquality.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import eu.bbmri_eric.quality.server.dataquality.dto.AgentDTO;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

@Component
final class AgentLinkBuilder {

  public EntityModel<AgentDTO> toModel(AgentDTO agentDto) {
    String selfHref =
        linkTo(methodOn(AgentController.class).findById(agentDto.getId(), null))
            .toUriComponentsBuilder()
            .replaceQuery(null)
            .build()
            .toUriString();

    return EntityModel.of(agentDto)
        .add(Link.of(selfHref).withSelfRel())
        .add(linkTo(AgentController.class).withRel("agents"));
  }

  public CollectionModel<EntityModel<AgentDTO>> toCollectionModel(List<AgentDTO> agents) {
    var entityModels = agents.stream().map(this::toModel).toList();

    return CollectionModel.of(entityModels).add(linkTo(AgentController.class).withSelfRel());
  }
}
