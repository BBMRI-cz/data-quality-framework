package eu.bbmri_eric.quality.agent.dataquality.config;

import eu.bbmri_eric.quality.agent.dataquality.DataStore;
import eu.bbmri_eric.quality.agent.dataquality.DataStoreFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
class DataStoreConfiguration {
  @Bean
  @Primary
  public DataStore dataStore(DataStoreFactory dataStoreFactory) {
    return dataStoreFactory.resolveDataStore();
  }
}


