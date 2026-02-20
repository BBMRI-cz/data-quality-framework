package eu.bbmri_eric.quality.server.setting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SettingServiceImpl implements SettingService {

  private final ModelMapper modelMapper;
  private final SettingRepository settingRepository;
  private final ObjectMapper objectMapper;
  private final OidcIssuerProvider oidcIssuerProvider;
  private final ApplicationEventPublisher eventPublisher;

  public SettingServiceImpl(
      SettingRepository settingRepository,
      ModelMapper modelMapper,
      ObjectMapper objectMapper,
      @Lazy OidcIssuerProvider oidcIssuerProvider,
      ApplicationEventPublisher eventPublisher) {
    this.settingRepository = settingRepository;
    this.modelMapper = modelMapper;
    this.objectMapper = objectMapper;
    this.oidcIssuerProvider = oidcIssuerProvider;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public SettingDTO getSettings() {
    Map<String, String> values = loadSettingsMap();
    return modelMapper.map(values, SettingDTO.class);
  }

  @Override
  public OidcSettingsDTO getOidcSettings() {
    Map<String, String> values = loadSettingsMap();
    return modelMapper.map(values, OidcSettingsDTO.class);
  }

  @Override
  public SettingDTO updateSettings(SettingDTO dto) {
    updateSettingsFromDto(dto);
    return dto;
  }

  @Override
  public OidcSettingsDTO updateOidcSettings(OidcSettingsDTO dto) {
    updateSettingsFromDto(dto);
    oidcIssuerProvider.clearCache();
    eventPublisher.publishEvent(new OidcSettingsUpdatedEvent());

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

  private void updateSetting(String name, String value) {
    if (value == null) {
      return;
    }
    Setting setting =
        settingRepository
            .findById(name)
            .orElseThrow(() -> new IllegalStateException("Setting not found: " + name));
    setting.setValue(value);
    settingRepository.save(setting);
  }

  private Map<String, String> loadSettingsMap() {
    return StreamSupport.stream(settingRepository.findAll().spliterator(), false)
        .filter(setting -> setting.getValue() != null)
        .collect(Collectors.toMap(Setting::getName, Setting::getValue));
  }
}
