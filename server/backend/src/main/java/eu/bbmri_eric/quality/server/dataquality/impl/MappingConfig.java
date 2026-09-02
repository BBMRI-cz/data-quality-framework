package eu.bbmri_eric.quality.server.dataquality.impl;

import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheck;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckKeyword;
import eu.bbmri_eric.quality.server.dataquality.domain.QualityCheckResult;
import eu.bbmri_eric.quality.server.dataquality.domain.Report;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckDetailedDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.QualityCheckResultDTO;
import eu.bbmri_eric.quality.server.dataquality.dto.ReportDTO;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import org.modelmapper.Converter;
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
    Converter<Set<QualityCheckKeyword>, List<String>> qualityCheckKeywordsConverter =
        context ->
            context.getSource() == null
                ? List.of()
                : context.getSource().stream()
                    .map(QualityCheckKeyword::getKeyword)
                    .sorted()
                    .toList();

    modelMapper.addMappings(
        new PropertyMap<QualityCheck, QualityCheckDTO>() {
          @Override
          protected void configure() {
            map(source.getId(), destination.getId());
            using(qualityCheckKeywordsConverter)
                .map(source.getKeywords(), destination.getKeywords());
          }
        });
    modelMapper.addMappings(
        new PropertyMap<QualityCheck, QualityCheckDetailedDTO>() {
          @Override
          protected void configure() {
            map(source.getId(), destination.getId());
            using(qualityCheckKeywordsConverter)
                .map(source.getKeywords(), destination.getKeywords());
          }
        });
    modelMapper.addMappings(
        new PropertyMap<QualityCheckResult, QualityCheckResultDTO>() {
          @Override
          protected void configure() {
            map(source.getVersion().getHash(), destination.getHash());
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
            map(source.getTotalPatients(), destination.getTotalPatients());
            map(source.getTotalSamples(), destination.getTotalSamples());
            map(source.getQualityCheckResults(), destination.getResults());
          }
        });
  }
}
