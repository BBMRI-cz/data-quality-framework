package eu.bbmri_eric.quality.server.dataquality.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import eu.bbmri_eric.quality.server.dataquality.dto.ReportDTO;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
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
}
