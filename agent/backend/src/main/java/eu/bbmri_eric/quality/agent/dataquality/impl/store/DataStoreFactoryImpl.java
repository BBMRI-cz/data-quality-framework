package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.DataStoreFactory;
import eu.bbmri_eric.quality.agent.settings.DatabaseType;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import eu.bbmri_eric.quality.agent.settings.event.SettingsUpdatedEvent;
import java.util.Base64;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class DataStoreFactoryImpl implements DataStoreFactory {
  private final SettingsService settingsService;
  private final BlazeFHIRStore fhirDataStore;
  private OmopDataStore omopDataStore;
  private volatile DataStore currentDataStore;

  DataStoreFactoryImpl(SettingsService settingsService, RestTemplateBuilder restTemplateBuilder) {
    this.settingsService = settingsService;
    this.fhirDataStore = new BlazeFHIRStore(restTemplateBuilder);
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
    if (databaseType == DatabaseType.SQL) {
      this.omopDataStore = createSqlDataStore(settings);
      return omopDataStore;
    }
    return fhirDataStore;
  }

  private OmopDataStore createSqlDataStore(SettingsDTO settings) {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setUrl(settings.getSqlUrl());
    dataSource.setUsername(settings.getSqlUsername());
    String password = settings.getSqlPassword();
    if (password != null && !password.isBlank()) {
      try {
        dataSource.setPassword(new String(Base64.getDecoder().decode(password)));
      } catch (IllegalArgumentException e) {
        log.warn("SQL password is not valid Base64, using raw value");
        dataSource.setPassword(password);
      }
    }
    log.info(
        "Creating SQL data store for URL: {} with user: {}",
        settings.getSqlUrl(),
        settings.getSqlUsername());
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    return new OmopDataStore(jdbcTemplate);
  }
}
