package eu.bbmri_eric.quality.agent.dataquality.config;

import eu.bbmri_eric.quality.agent.dataquality.domain.DataQualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.domain.Result;
import eu.bbmri_eric.quality.agent.dataquality.dto.ResultDTO;
import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResultMapper {

  private final ModelMapper modelMapper;

  public ResultMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  @PostConstruct
  public void addMappings() {
    modelMapper.addMappings(
        new PropertyMap<ResultDTO, Result>() {
          @Override
          protected void configure() {
            map().setRawValue(source.rawResult());
            map().setPatients(source.idSet());
          }
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
              boolean rootSourceIsDataQualityCheck =
                  context.getParent() != null
                      && context.getParent().getSource() instanceof DataQualityCheck;
              boolean rootDestinationIsResult =
                  context.getParent() != null
                      && context.getParent().getDestination() instanceof Result;
              return !(destinationIsId && rootSourceIsDataQualityCheck && rootDestinationIsResult);
            });
  }
}
