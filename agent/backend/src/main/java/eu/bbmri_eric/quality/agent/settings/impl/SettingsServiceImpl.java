package eu.bbmri_eric.quality.agent.settings.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.common.EventPublisher;
import eu.bbmri_eric.quality.agent.settings.NoiseMechanism;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.domain.Settings;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import eu.bbmri_eric.quality.agent.settings.event.SettingsUpdatedEvent;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for managing application settings. Persists settings to the database and
 * publishes events so other components (e.g. FHIR client, differential privacy engine) can react to
 * changes without tight coupling.
 */
@Service
@Transactional
public class SettingsServiceImpl implements SettingsService {

  private static final Logger log = LoggerFactory.getLogger(SettingsServiceImpl.class);

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
    SettingsDTO updated = getSettings();
    if (updated.getNoiseMechanism() == NoiseMechanism.GAUSSIAN
        && updated.getEpsilon() != null
        && updated.getEpsilon() > 1.0) {
      throw new IllegalArgumentException(
          "Epsilon must be less than or equal to 1.0 when using Gaussian noise");
    }
    eventPublisher.publishEvent(new SettingsUpdatedEvent(updated));
    log.info(
        "Settings updated: databaseType={}, noiseMechanism={}, epsilon={}, delta={}, minThreshold={}",
        updated.getDatabaseType(),
        updated.getNoiseMechanism(),
        updated.getEpsilon(),
        updated.getDelta(),
        updated.getMinThreshold());
    return dto;
  }

  private void updateSetting(String name, String value) {
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
