package eu.bbmri_eric.quality.agent.dataquality.config;

import eu.bbmri_eric.quality.agent.dataquality.domain.DataQualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResultMapper {

  private final ModelMapper modelMapper;

  public ResultMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  @PostConstruct
  public void addMappings() {
    modelMapper
        .typeMap(ResultDTO.class, Result.class)
        .setPostConverter(
            context -> {
              ResultDTO source = context.getSource();
              Result destination = context.getDestination();
              destination.setRawValue(source.rawResult());
              destination.setPatients(source.idSet());
              return destination;
            });

    modelMapper
        .getConfiguration()
        .setPropertyCondition(
            context -> {
              var mapping = context.getMapping();
              if (mapping == null) {
                return true;
              }
              boolean destinationIsId = "id".equals(mapping.getLastDestinationProperty().getName());
              boolean isErrorMessageMapping =
                  "error".equals(mapping.getLastDestinationProperty().getName());
              boolean rootSourceIsDataQualityCheck =
                  context.getParent() != null
                      && context.getParent().getSource() instanceof DataQualityCheck;
              boolean rootDestinationIsResult =
                  context.getParent() != null
                      && context.getParent().getDestination() instanceof Result;
              return !((destinationIsId || isErrorMessageMapping)
                  && rootSourceIsDataQualityCheck
                  && rootDestinationIsResult);
            });
  }
}
