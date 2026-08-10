package eu.bbmri_eric.quality.agent.dataquality.config;

import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckUpdateDTO;
import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QualityCheckMapper {

  private final ModelMapper modelMapper;

  public QualityCheckMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  @PostConstruct
  public void addMappings() {
    modelMapper
        .typeMap(QualityCheckUpdateDTO.class, QualityCheck.class)
        .setPropertyCondition(
            context -> {
              var mapping = context.getMapping();
              if (mapping == null) {
                return true;
              }
              return !"id".equals(mapping.getLastDestinationProperty().getName());
            });
  }
}
