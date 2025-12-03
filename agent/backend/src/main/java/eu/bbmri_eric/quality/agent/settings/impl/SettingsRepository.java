package eu.bbmri_eric.quality.agent.settings.impl;

import eu.bbmri_eric.quality.agent.settings.domain.Settings;
import org.springframework.data.repository.CrudRepository;

public interface SettingsRepository extends CrudRepository<Settings, String> {}
