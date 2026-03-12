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
                  FHIR® Server
                </h2>
                <p class="text-muted mb-0">
                  Configure your FHIR® server connection to access the data
                </p>
              </div>
            </div>

            <form class="settings-form" @submit.prevent="saveFhirSettings">
              <FormField
                id="fhirUrl"
                v-model="fhirSettings.url"
                type="url"
                label="Server URL"
                icon="bi-link-45deg"
                placeholder="https://fhir-server.example.com/fhir"
                help-text="The base URL of your FHIR® server endpoint"
                required
              />

              <FormField
                id="fhirUsername"
                v-model="fhirSettings.username"
                label="Username"
                icon="bi-person"
                placeholder="Enter username"
                help-text="Username for authenticating with the FHIR® server"
                required
                autocomplete="username"
              />

              <FormField
                id="fhirPassword"
                v-model="fhirSettings.password"
                type="password"
                label="Password"
                icon="bi-key"
                placeholder="Enter password"
                help-text="Password for authenticating with the FHIR® server"
                required
                autocomplete="current-password"
              />

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
  import { ref, reactive, onMounted } from 'vue';
  import { useSettingsStore } from '@/stores/settingsStore.js';
  import PageHeader from '@/components/PageHeader.vue';
  import HealthStatusBanner from '@/components/HealthStatusBanner.vue';
  import { FormField, FormActions } from '@/components/forms';
  import { useHealthStore } from '@/stores/healthStore.js';
  import { notificationService } from '@/services/notificationService.js';

  const settingsStore = useSettingsStore();
  const healthStore = useHealthStore();

  const isSaving = ref(false);

  const fhirSettings = reactive({
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

  async function saveFhirSettings() {
    isSaving.value = true;

    try {
      const payload = {
        ...settingsStore.settings,
        fhirUrl: fhirSettings.url,
        fhirUsername: fhirSettings.username,
        fhirPassword: encodeBase64(fhirSettings.password),
      };

      await settingsStore.updateSettings(payload);
      notificationService.success(
        'Settings Saved',
        'Your FHIR® server settings have been updated successfully'
      );
      await healthStore.checkHealth();
    } catch (error) {
      console.error('Error saving FHIR® settings:', error);
      notificationService.error('Save Failed', 'Unable to save settings. Please try again.');
    } finally {
      isSaving.value = false;
    }
  }

  async function loadSettings() {
    try {
      const data = await settingsStore.fetchSettings();
      if (data) {
        fhirSettings.url = data.fhirUrl || '';
        fhirSettings.username = data.fhirUsername || '';
        fhirSettings.password = data.fhirPassword ? decodeBase64(data.fhirPassword) : '';
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
