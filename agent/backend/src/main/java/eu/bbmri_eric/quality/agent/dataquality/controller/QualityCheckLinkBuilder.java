package eu.bbmri_eric.quality.agent.dataquality.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import eu.bbmri_eric.quality.agent.common.LinkBuilder;
import eu.bbmri_eric.quality.agent.common.dto.PageResponse;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckFilterDTO;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.stereotype.Component;

@Component
class QualityCheckLinkBuilder {

  public EntityModel<QualityCheckDTO> toModel(QualityCheckDTO qualityCheck) {
    return EntityModel.of(qualityCheck)
        .add(
            linkTo(methodOn(QualityCheckController.class).findById(qualityCheck.getId()))
                .withSelfRel())
        .add(
            linkTo(
                    methodOn(QualityCheckController.class)
                        .findAll(
                            new QualityCheckFilterDTO(
                                0, 20, null, QualityCheckFilterDTO.SortOrder.ASC)))
                .withRel("quality-checks"));
  }

  public PagedModel<EntityModel<QualityCheckDTO>> toPagedModel(
      PageResponse<QualityCheckDTO> pageResponse, QualityCheckFilterDTO filter) {
    List<EntityModel<QualityCheckDTO>> models =
        pageResponse.getContent().stream().map(this::toModel).collect(Collectors.toList());

    PagedModel.PageMetadata metadata =
        new PagedModel.PageMetadata(
            pageResponse.getSize(),
            pageResponse.getPage(),
            pageResponse.getTotalElements(),
            pageResponse.getTotalPages());

    PagedModel<EntityModel<QualityCheckDTO>> pagedModel = PagedModel.of(models, metadata);
    Link selfLink = linkTo(methodOn(QualityCheckController.class).findAll(filter)).withSelfRel();

    List<Link> paginationLinks = LinkBuilder.getPageLinks(selfLink.toUri(), filter, pageResponse);
    pagedModel.add(paginationLinks);

    return pagedModel;
  }
}
