package eu.bbmri_eric.quality.agent.dataquality.impl.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.settings.DatabaseType;
import eu.bbmri_eric.quality.agent.settings.SettingsService;
import eu.bbmri_eric.quality.agent.settings.dto.SettingsDTO;
import eu.bbmri_eric.quality.agent.settings.event.SettingsUpdatedEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.web.client.RestTemplateBuilder;

class DataStoreFactoryImplTest {

  @TempDir Path tempDir;

  @Test
  void calciteDirectoryUrl_resolvesToSqlDataStoreOverCsv() throws Exception {
    Files.writeString(
        tempDir.resolve("person.csv"),
        "\"person_id\",\"name\"\n\"1\",\"Alice\"\n\"2\",\"Bob\"\n\"3\",\"Chad\"\n");
    DataStoreFactoryImpl factory =
        new DataStoreFactoryImpl(mock(SettingsService.class), mock(RestTemplateBuilder.class));
    SettingsDTO settings =
        SettingsDTO.builder()
            .databaseType(DatabaseType.SQL)
            .sqlUrl("jdbc:calcite:directory=" + tempDir.toAbsolutePath())
            .build();
    factory.onSettingsUpdated(new SettingsUpdatedEvent(settings));

    DataStore store = factory.resolveDataStore();
    assertInstanceOf(OmopDataStore.class, store);
    OmopDataStore omop = (OmopDataStore) store;
    assertEquals(3, omop.countPatients());
  }

  @Test
  void nonCalciteUrl_usesDriverManagerDataSource() {
    DataStoreFactoryImpl factory =
        new DataStoreFactoryImpl(mock(SettingsService.class), mock(RestTemplateBuilder.class));
    SettingsDTO settings =
        SettingsDTO.builder()
            .databaseType(DatabaseType.SQL)
            .sqlUrl("jdbc:postgresql://localhost:5432/quality")
            .sqlUsername("user")
            .build();
    factory.onSettingsUpdated(new SettingsUpdatedEvent(settings));

    DataStore store = factory.resolveDataStore();
    assertInstanceOf(OmopDataStore.class, store);
  }
}
