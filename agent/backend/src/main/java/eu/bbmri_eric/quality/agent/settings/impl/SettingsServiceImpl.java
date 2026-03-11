package eu.bbmri_eric.quality.agent.settings.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.domain.Settings;
import eu.bbmri_eric.quality.agent.settings.dto.DiffPrivacySettingsDTO;
import eu.bbmri_eric.quality.agent.settings.dto.FhirSettingsDTO;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import eu.bbmri_eric.quality.agent.settings.event.DiffPrivacySettingsUpdateEvent;
import eu.bbmri_eric.quality.agent.settings.event.FhirSettingsUpdateEvent;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for managing application settings. This service provides methods to
 * retrieve and update settings, as well as specific methods for handling FHIR and differential
 * privacy settings.
 */
@Service
@Transactional
public class SettingsServiceImpl implements SettingsService {

  private final ObjectMapper objectMapper;
  private final SettingsRepository settingsRepository;
  private final EventPublisher eventPublisher;

  public SettingsServiceImpl(
      SettingsRepository settingsRepository,
      EventPublisher eventPublisher,
      ObjectMapper objectMapper) {
    this.settingsRepository = settingsRepository;
    this.eventPublisher = eventPublisher;
    this.objectMapper = objectMapper;
  }

  @Override
  public SettingsDTO getSettings() {
    Map<String, String> values = loadSettingsMap();
    return objectMapper.convertValue(values, SettingsDTO.class);
  }

  @Override
  public SettingsDTO updateSettings(SettingsDTO dto) {
    updateSettingsFromDto(dto);
    return dto;
  }

  private void updateSetting(String name, String value) {
    if (value == null) {
      return;
    }
    Settings setting =
        settingsRepository
            .findById(name)
            .orElseThrow(() -> new IllegalStateException("Setting not found: " + name));
    setting.setValue(value);
    settingsRepository.save(setting);
  }

  private Map<String, String> loadSettingsMap() {
    return StreamSupport.stream(settingsRepository.findAll().spliterator(), false)
        .collect(Collectors.toMap(Settings::getName, Settings::getValue));
  }

  @Override
  public DiffPrivacySettingsDTO getDiffPrivacySettings() {
    Map<String, String> values = loadSettingsMap();
    return objectMapper.convertValue(values, DiffPrivacySettingsDTO.class);
  }

  @Override
  public DiffPrivacySettingsDTO updateDiffPrivacySettings(DiffPrivacySettingsDTO dto) {
    updateSettingsFromDto(dto);
    eventPublisher.publishEvent(new DiffPrivacySettingsUpdateEvent(dto));
    return dto;
  }

  @Override
  public FhirSettingsDTO getFhirSettings() {
    Map<String, String> values = loadSettingsMap();
    return objectMapper.convertValue(values, FhirSettingsDTO.class);
  }

  @Override
  public FhirSettingsDTO updateFhirSettings(FhirSettingsDTO dto) {
    updateSettingsFromDto(dto);
    eventPublisher.publishEvent(new FhirSettingsUpdateEvent(dto));
    return dto;
  }

  private void updateSettingsFromDto(Object dto) {
    Map<String, Object> dtoMap = objectMapper.convertValue(dto, new TypeReference<>() {});
    dtoMap.forEach(
        (name, value) -> {
          if (value != null) {
            updateSetting(name, value.toString());
          }
        });
  }
}
