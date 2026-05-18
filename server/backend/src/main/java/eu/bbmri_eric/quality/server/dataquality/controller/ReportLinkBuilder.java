package eu.bbmri_eric.quality.server.dataquality.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import eu.bbmri_eric.quality.server.common.LinkBuilder;
import eu.bbmri_eric.quality.server.common.dto.FilterDTO;
import eu.bbmri_eric.quality.server.common.dto.PageResponse;
import eu.bbmri_eric.quality.server.dataquality.dto.ReportDTO;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.stereotype.Component;

/** HATEOAS link builder for Report resources. */
@Component
final class ReportLinkBuilder {

  public EntityModel<ReportDTO> toModel(ReportDTO reportDto) {
    return EntityModel.of(reportDto)
        .add(linkTo(methodOn(ReportController.class).findById(reportDto.getId())).withSelfRel())
        .add(linkTo(ReportController.class).withRel("reports"));
  }

  public CollectionModel<EntityModel<ReportDTO>> toCollectionModel(List<ReportDTO> reports) {
    var entityModels = reports.stream().map(this::toModel).toList();

    return CollectionModel.of(entityModels).add(linkTo(ReportController.class).withSelfRel());
  }

  public PagedModel<EntityModel<ReportDTO>> toPagedModel(
      PageResponse<ReportDTO> pageResponse, FilterDTO filter) {
    List<EntityModel<ReportDTO>> reportModels =
        pageResponse.getContent().stream().map(this::toModel).collect(Collectors.toList());

    PagedModel.PageMetadata metadata =
        new PagedModel.PageMetadata(
            pageResponse.getSize(),
            pageResponse.getPage(),
            pageResponse.getTotalElements(),
            pageResponse.getTotalPages());

    PagedModel<EntityModel<ReportDTO>> pagedModel = PagedModel.of(reportModels, metadata);
    Link selfLink = linkTo(methodOn(ReportController.class).findAll(filter)).withSelfRel();

    pagedModel.add(selfLink);
    List<Link> paginationLinks = LinkBuilder.getPageLinks(selfLink.toUri(), filter, pageResponse);
    pagedModel.add(paginationLinks);

    return pagedModel;
  }

  public PagedModel<EntityModel<ReportDTO>> toPagedModelForAgent(
      PageResponse<ReportDTO> pageResponse, String agentId, FilterDTO filter) {
    List<EntityModel<ReportDTO>> reportModels =
        pageResponse.getContent().stream().map(this::toModel).collect(Collectors.toList());

    PagedModel.PageMetadata metadata =
        new PagedModel.PageMetadata(
            pageResponse.getSize(),
            pageResponse.getPage(),
            pageResponse.getTotalElements(),
            pageResponse.getTotalPages());

    PagedModel<EntityModel<ReportDTO>> pagedModel = PagedModel.of(reportModels, metadata);
    Link selfLink =
        linkTo(methodOn(ReportController.class).findByAgentId(agentId, filter)).withSelfRel();

    pagedModel.add(selfLink);
    List<Link> paginationLinks = LinkBuilder.getPageLinks(selfLink.toUri(), filter, pageResponse);
    pagedModel.add(paginationLinks);

    return pagedModel;
  }
}
