package eu.bbmri_eric.quality.agent.dataquality.config;

import eu.bbmri_eric.quality.agent.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckBulkUpdateDTO;
import eu.bbmri_eric.quality.agent.dataquality.dto.QualityCheckUpdateDTO;
import jakarta.annotation.PostConstruct;
import org.modelmapper.Condition;
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
    Condition<Object, Object> skipNullAndId =
        context -> {
          // Skip null source values so updates behave as partial updates
          if (context.getSource() == null) {
            return false;
          }
          var mapping = context.getMapping();
          if (mapping == null) {
            return true;
          }
          return !"id".equals(mapping.getLastDestinationProperty().getName());
        };
    modelMapper
        .typeMap(QualityCheckUpdateDTO.class, QualityCheck.class)
        .setPropertyCondition(skipNullAndId);
    modelMapper
        .typeMap(QualityCheckBulkUpdateDTO.class, QualityCheck.class)
        .setPropertyCondition(skipNullAndId);
  }
}
