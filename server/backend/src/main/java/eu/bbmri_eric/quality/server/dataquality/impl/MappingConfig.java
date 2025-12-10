package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckResult;
import eu.bbmri_eric.quality.server.dataquality.domain.Report;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckResultDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ReportDTO;
import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

@Component
public class MappingConfig {
  private final ModelMapper modelMapper;

  public MappingConfig(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  @PostConstruct
  private void addMappings() {
    modelMapper.addMappings(
        new PropertyMap<QualityCheckResult, QualityCheckResultDTO>() {
          @Override
          protected void configure() {
            map(source.getQualityCheck().getHash(), destination.getHash());
            map(source.getQualityCheck().getName(), destination.getName());
            map(source.getResult(), destination.getResult());
          }
        });
    modelMapper.addMappings(
        new PropertyMap<Report, ReportDTO>() {
          @Override
          protected void configure() {
            map(source.getId(), destination.getId());
            map(source.getTimestamp(), destination.getTimestamp());
            map(source.getAgentId(), destination.getAgentId());
            map(source.getQualityCheckResults(), destination.getResults());
          }
        });
  }
}
