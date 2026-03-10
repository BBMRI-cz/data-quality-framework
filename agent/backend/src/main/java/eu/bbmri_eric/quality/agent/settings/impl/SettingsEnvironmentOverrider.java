package eu.bbmri_eric.quality.agent.settings.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Overrides application settings on startup based on environment variables.
 *
 * <p>This component looks for environment variables starting with {@code APP_SETTING_}. It
 * normalizes the names (lowercase, removing underscores, dots, and dashes) and matches them against
 * the fields in {@link eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO}.
 *
 * <p>For example, an environment variable {@code APP_SETTING_FHIR_URL=http://example.com} will
 * override the {@code fhirUrl} setting.
 */
@Component
class SettingsEnvironmentOverrider implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(SettingsEnvironmentOverrider.class);
  private static final String ENV_PREFIX = "APP_SETTING_";

  private final SettingsService settingsService;
  private final ObjectMapper objectMapper;

  public SettingsEnvironmentOverrider(SettingsService settingsService, ObjectMapper objectMapper) {
    this.settingsService = settingsService;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Map<String, String> overrides = getNormalizedOverrides();
    Map<String, Object> settings = getSettingsMap();
    if (applyOverrides(settings, overrides)) {
      saveSettings(settings);
    }
  }

  private Map<String, String> getNormalizedOverrides() {
    Map<String, String> normalized = new HashMap<>();
    getSystemEnv().entrySet().stream()
        .filter(entry -> entry.getKey().toUpperCase().startsWith(ENV_PREFIX))
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              String key = entry.getKey();
              String normalizedKey = normalize(key.substring(ENV_PREFIX.length()));
              if (normalized.containsKey(normalizedKey)) {
                log.warn(
                    "Duplicate environment variable override for setting '{}'. Variable '{}' is ignored because a higher precedence variable (lexicographically earlier) was already processed.",
                    normalizedKey,
                    key);
              } else {
                normalized.put(normalizedKey, entry.getValue());
              }
            });
    return normalized;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getSettingsMap() {
    return objectMapper.convertValue(settingsService.getSettings(), Map.class);
  }

  private boolean applyOverrides(Map<String, Object> settings, Map<String, String> overrides) {
    boolean changed = false;
    for (Map.Entry<String, Object> entry : settings.entrySet()) {
      String normalizedKey = normalize(entry.getKey());
      if (overrides.containsKey(normalizedKey)) {
        String newValue = overrides.get(normalizedKey);
        String currentValue = entry.getValue() != null ? entry.getValue().toString() : null;

        if (!Objects.equals(newValue, currentValue)) {
          log.info("Overriding setting '{}' from environment variable", entry.getKey());
          entry.setValue(newValue);
          changed = true;
        }
      }
    }
    return changed;
  }

  private void saveSettings(Map<String, Object> settingsMap) {
    SettingsDTO updatedSettings = objectMapper.convertValue(settingsMap, SettingsDTO.class);
    settingsService.updateSettings(updatedSettings);
  }

  protected Map<String, String> getSystemEnv() {
    return System.getenv();
  }

  private String normalize(String key) {
    if (key == null) {
      return "";
    }
    return key.toLowerCase().replace("_", "").replace(".", "").replace("-", "");
  }
}
