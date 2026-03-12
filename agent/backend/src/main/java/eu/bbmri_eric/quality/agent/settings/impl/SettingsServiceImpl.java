package eu.bbmri_eric.quality.agent.settings.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.domain.Settings;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import eu.bbmri_eric.quality.agent.settings.event.SettingsUpdatedEvent;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for managing application settings. Persists settings to the database and
 * publishes events so other components (e.g. FHIR client, differential privacy engine) can react
 * to changes without tight coupling.
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
    eventPublisher.publishEvent(new SettingsUpdatedEvent(dto));
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
