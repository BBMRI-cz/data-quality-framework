package eu.bbmri_eric.quality.agent.audit.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.quality.agent.audit.dto.AuditLogDTO;
import eu.bbmri_eric.quality.agent.audit.dto.AuditLogFilterDTO;
import eu.bbmri_eric.quality.agent.common.LinkBuilder;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.stereotype.Component;

@Component
class AuditLogLinkBuilder {

  public PagedModel<EntityModel<AuditLogDTO>> toPagedModel(
      PageResponse<AuditLogDTO> pageResponse, AuditLogFilterDTO filter) {
    List<EntityModel<AuditLogDTO>> entryModels =
        pageResponse.getContent().stream().map(EntityModel::of).collect(Collectors.toList());

    PagedModel.PageMetadata metadata =
        new PagedModel.PageMetadata(
            pageResponse.getSize(),
            pageResponse.getPage(),
            pageResponse.getTotalElements(),
            pageResponse.getTotalPages());

    PagedModel<EntityModel<AuditLogDTO>> pagedModel = PagedModel.of(entryModels, metadata);
    Link selfLink = linkTo(methodOn(AuditController.class).findAll(filter)).withSelfRel();

    List<Link> paginationLinks = LinkBuilder.getPageLinks(selfLink.toUri(), filter, pageResponse);
    pagedModel.add(paginationLinks);

    return pagedModel;
  }
}
