<template>
  <div class="settings-page">
    <PageHeader
      title="Settings"
      mobile-title="Settings"
      subtitle="Manage your application preferences and configurations"
      icon="bi bi-gear"
    />

    <div class="page-content">
      <div class="settings-card-container">
        <HealthStatusBanner />

        <div class="settings-card">
          <div class="settings-section">
            <div class="section-header">
              <div>
                <h2 class="section-title">
                  <i class="bi bi-database"></i>
                  Connection Settings
                </h2>
                <p class="text-muted mb-0">
                  Choose the data source and configure the connection details
                </p>
                <a
                  class="docs-link"
                  href="https://fdqf.bbmri-eric.eu/user/data-sources"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Documentation
                  <i class="bi bi-box-arrow-up-right"></i>
                </a>
              </div>
            </div>

            <form class="settings-form" @submit.prevent="saveSettings">
              <DatabaseTypeSelect v-model="databaseType" :options="databaseTypeOptions" required />

              <FhirSettingsFields v-if="isFhir" :settings="fhirSettings" :required="isFhir" />
              <SqlSettingsFields v-else :settings="sqlSettings" :required="isSql" />

              <FormActions
                :loading="isSaving"
                :show-cancel-button="false"
                :save-text="isSaving ? 'Saving...' : 'Save Changes'"
                save-icon="bi-check-circle"
              />
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, reactive, onMounted, computed } from 'vue';
  import { useSettingsStore } from '@/stores/settingsStore.js';
  import PageHeader from '@/components/PageHeader.vue';
  import HealthStatusBanner from '@/components/HealthStatusBanner.vue';
  import { FormActions } from '@/components/forms';
  import { useHealthStore } from '@/stores/healthStore.js';
  import { notificationService } from '@/services/notificationService.js';
  import DatabaseTypeSelect from '@/components/settings/DatabaseTypeSelect.vue';
  import FhirSettingsFields from '@/components/settings/FhirSettingsFields.vue';
  import SqlSettingsFields from '@/components/settings/SqlSettingsFields.vue';

  const settingsStore = useSettingsStore();
  const healthStore = useHealthStore();

  const isSaving = ref(false);
  const databaseType = ref('FHIR');

  const databaseTypeOptions = [
    { label: 'FHIR', value: 'FHIR' },
    { label: 'SQL', value: 'SQL' },
  ];

  const isFhir = computed(() => databaseType.value === 'FHIR');
  const isSql = computed(() => databaseType.value === 'SQL');

  const fhirSettings = reactive({
    url: '',
    username: '',
    password: '',
  });

  const sqlSettings = reactive({
    url: '',
    username: '',
    password: '',
  });

  function decodeBase64(encoded) {
    try {
      return atob(encoded);
    } catch (e) {
      console.error('Error decoding Base64:', e);
      return '';
    }
  }

  function encodeBase64(text) {
    try {
      return btoa(text);
    } catch (e) {
      console.error('Error encoding Base64:', e);
      return '';
    }
  }

  async function saveSettings() {
    isSaving.value = true;

    try {
      const payload = {
        ...settingsStore.settings,
        databaseType: databaseType.value,
        fhirUrl: fhirSettings.url,
        fhirUsername: fhirSettings.username,
        fhirPassword: encodeBase64(fhirSettings.password),
        sqlUrl: sqlSettings.url,
        sqlUsername: sqlSettings.username,
        sqlPassword: encodeBase64(sqlSettings.password),
      };

      await settingsStore.updateSettings(payload);
      notificationService.success(
        'Settings Saved',
        'Your connection settings have been updated successfully'
      );
      await healthStore.checkHealth();
    } catch (error) {
      console.error('Error saving connection settings:', error);
      notificationService.error('Save Failed', 'Unable to save settings. Please try again.');
    } finally {
      isSaving.value = false;
    }
  }

  async function loadSettings() {
    try {
      const data = await settingsStore.fetchSettings();
      if (data) {
        databaseType.value = data.databaseType || 'FHIR';
        fhirSettings.url = data.fhirUrl || '';
        fhirSettings.username = data.fhirUsername || '';
        fhirSettings.password = data.fhirPassword ? decodeBase64(data.fhirPassword) : '';
        sqlSettings.url = data.sqlUrl || '';
        sqlSettings.username = data.sqlUsername || '';
        sqlSettings.password = data.sqlPassword ? decodeBase64(data.sqlPassword) : '';
      }
    } catch (error) {
      console.error('Error loading settings:', error);
    }
  }

  onMounted(() => {
    loadSettings();
  });
</script>

<style scoped>
  .settings-page {
    min-height: 100%;
    padding: var(--spacing-xl);
  }

  .page-content {
    max-width: 1400px;
    margin: 0 auto;
  }

  .settings-card-container {
    max-width: 900px;
    margin: 0 auto;
  }

  .settings-card {
    background: var(--bg-card);
    border-radius: var(--radius-xl);
    box-shadow: var(--shadow-sm);
  }

  .settings-section {
    padding: var(--spacing-2xl);
  }

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: var(--spacing-xl);
  }

  .section-title {
    font-size: 1.5rem;
    font-weight: 700;
    color: var(--color-gray-800);
    margin: 0 0 var(--spacing-sm) 0;
    display: flex;
    align-items: center;
    gap: 0.75rem;
  }

  .section-title i {
    color: var(--color-primary);
  }

  .docs-link {
    color: var(--color-primary);
    text-decoration: none;
    display: inline-flex;
    align-items: center;
    gap: 0.375rem;
    font-size: 0.875rem;
    margin-top: var(--spacing-xs);
    flex-shrink: 0;
  }

  .docs-link:hover {
    text-decoration: underline;
  }

  .settings-form {
    max-width: 600px;
  }

  /* Override FormActions background for settings context */
  :deep(.form-actions) {
    background-color: transparent;
    border-radius: 0;
    padding: var(--spacing-lg) 0 0 0;
    border-top: 2px solid var(--color-gray-100);
    margin-top: var(--spacing-xl);
    justify-content: flex-start;
  }

  @media (max-width: 768px) {
    .settings-page {
      padding: var(--spacing-md);
    }

    .settings-section {
      padding: var(--spacing-lg);
    }

    .section-title {
      font-size: 1.35rem;
    }

    .settings-form {
      max-width: 100%;
    }
  }

  @media (max-width: 576px) {
    .settings-page {
      padding: 0.75rem;
    }

    .settings-section {
      padding: 1.25rem;
    }

    .section-header {
      margin-bottom: var(--spacing-lg);
    }

    .section-title {
      font-size: 1.2rem;
      gap: var(--spacing-sm);
    }
  }
</style>
