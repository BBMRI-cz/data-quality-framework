package eu.bbmri_eric.quality.agent.dataquality;

/** Factory for selecting the active {@link DataStore} based on current settings. */
public interface DataStoreFactory {
  /**
   * Resolves the data store that should be used based on current settings.
   *
   * @return the resolved {@link DataStore}
   */
  DataStore resolveDataStore();
}

