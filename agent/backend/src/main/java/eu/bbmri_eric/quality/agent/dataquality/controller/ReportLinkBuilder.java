package eu.bbmri_eric.quality.agent.dataquality.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.quality.agent.common.LinkBuilder;
import eu.bbmri_eric.quality.agent.common.dto.FilterDTO;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import eu.bbmri_eric.quality.agent.dataquality.dto.ReportDTO;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.stereotype.Component;

@Component
class ReportLinkBuilder {

  public EntityModel<ReportDTO> toModel(ReportDTO report) {
    return EntityModel.of(report)
        .add(linkTo(methodOn(ReportController.class).findById(report.getId())).withSelfRel())
        .add(
            linkTo(
                    methodOn(ReportController.class)
                        .findAll(new FilterDTO(0, 20, null, FilterDTO.SortOrder.ASC)))
                .withRel("reports"));
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

    List<Link> paginationLinks = LinkBuilder.getPageLinks(selfLink.toUri(), filter, pageResponse);
    pagedModel.add(paginationLinks);

    return pagedModel;
  }
}
