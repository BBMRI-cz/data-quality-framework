package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.DataStoreFactory;
import eu.bbmri_eric.quality.agent.settings.DatabaseType;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import eu.bbmri_eric.quality.agent.settings.event.SettingsUpdatedEvent;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class DataStoreFactoryImpl implements DataStoreFactory {
  private final SettingsService settingsService;
  private final BlazeFHIRStore fhirDataStore;
  private final SqlDataStore sqlDataStore;
  private volatile DataStore currentDataStore;

  DataStoreFactoryImpl(SettingsService settingsService, RestTemplateBuilder restTemplateBuilder) {
    this.settingsService = settingsService;
    this.fhirDataStore = new BlazeFHIRStore(restTemplateBuilder);
    this.sqlDataStore = new SqlDataStore();
  }

  @Override
  public DataStore resolveDataStore() {
    DataStore cached = currentDataStore;
    if (cached != null) {
      return cached;
    }
    return resolveFromSettings(settingsService.getSettings());
  }

  @EventListener
  void onSettingsUpdated(SettingsUpdatedEvent event) {
    checkSettings(event);
    SettingsDTO settings = event.getSettings();
    log.info("Settings updated, refreshing active data store");
    this.currentDataStore = resolveFromSettings(settings);
    if (settings.getDatabaseType() == DatabaseType.FHIR) {
      fhirDataStore.onSettingsUpdated(event);
    }
  }

  private void checkSettings(SettingsUpdatedEvent event) {
    if (Objects.isNull(event.getSettings())) {
      throw new IllegalArgumentException("Event contained null values, skipping reinitialization");
    }
  }

  private DataStore resolveFromSettings(SettingsDTO settings) {
    DatabaseType databaseType = settings != null ? settings.getDatabaseType() : null;
    if (databaseType == null) {
      log.warn("Database type not configured, defaulting to FHIR data store.");
      return fhirDataStore;
    }
    return databaseType == DatabaseType.SQL ? sqlDataStore : fhirDataStore;
  }
}
