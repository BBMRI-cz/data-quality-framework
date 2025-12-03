<template>
  <div class="settings-page">
    <PageHeader
      title="Settings"
      mobileTitle="Settings"
      subtitle="Manage your application preferences and configurations"
      icon="bi bi-gear-fill"
    />

    <div class="page-content">
      <div class="settings-card-container">
        <HealthStatusBanner/>

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

            <form @submit.prevent="saveFhirSettings" class="settings-form">
              <div class="form-group">
                <label for="fhirUrl" class="form-label">
                  Server URL
                </label>
                <input
                  type="url"
                  class="form-control"
                  id="fhirUrl"
                  v-model="fhirSettings.url"
                  placeholder="https://fhir-server.example.com/fhir"
                  required
                >
                <small class="form-help">
                  The base URL of your FHIR® server endpoint
                </small>
              </div>

              <div class="form-group">
                <label for="fhirUsername" class="form-label">
                  Username
                </label>
                <input
                  type="text"
                  class="form-control"
                  id="fhirUsername"
                  v-model="fhirSettings.username"
                  placeholder="Enter username"
                  required
                  autocomplete="username"
                >
                <small class="form-help">
                  Username for authenticating with the FHIR® server
                </small>
              </div>

              <div class="form-group">
                <label for="fhirPassword" class="form-label">
                  Password
                </label>
                <input
                  type="password"
                  class="form-control"
                  id="fhirPassword"
                  v-model="fhirSettings.password"
                  placeholder="Enter password"
                  required
                  autocomplete="current-password"
                >
                <small class="form-help">
                  Password for authenticating with the FHIR® server
                </small>
              </div>

              <div class="form-actions">
                <SaveButton
                  type="submit"
                  :loading="isSaving"
                  :text="isSaving ? 'Saving...' : 'Save Changes'"
                />
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import settingsStore from '../stores/settingsStore.js';
import PageHeader from '../components/PageHeader.vue';
import HealthStatusBanner from '../components/HealthStatusBanner.vue';
import SaveButton from '../components/SaveButton.vue';
import healthStore from '../stores/healthStore.js';
import { notificationService } from '../services/notificationService.js';

const isSaving = ref(false);

const fhirSettings = reactive({
  url: '',
  username: '',
  password: ''
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
      fhirUrl: fhirSettings.url,
      fhirUsername: fhirSettings.username,
      fhirPassword: encodeBase64(fhirSettings.password)
    };

    await settingsStore.updateSettings(payload);
    notificationService.success('Settings Saved', 'Your FHIR® server settings have been updated successfully');
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

.section-description {
  font-size: 0.95rem;
  color: var(--color-gray-500);
  margin: 0;
  line-height: 1.5;
}

.settings-form {
  max-width: 600px;
}

.form-group {
  margin-bottom: 1.75rem;
}

.form-label {
  display: block;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-gray-700);
  margin-bottom: var(--spacing-sm);
}

.form-control {
  width: 100%;
  padding: 0.75rem var(--spacing-md);
  font-size: 1rem;
  border: 2px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  transition: all var(--transition-base);
  background: var(--bg-card);
}

.form-control:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.form-help {
  display: block;
  margin-top: var(--spacing-sm);
  font-size: 0.875rem;
  color: var(--color-gray-500);
  line-height: 1.4;
}

.form-actions {
  margin-top: var(--spacing-xl);
  padding-top: var(--spacing-lg);
  border-top: 2px solid var(--color-gray-100);
}

.btn {
  padding: 0.875rem var(--spacing-xl);
  font-size: 1rem;
  font-weight: 600;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  display: inline-flex;
  align-items: center;
}

.btn-primary {
  background: var(--gradient-primary);
  color: white;
  box-shadow: var(--shadow-primary);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-primary-hover);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
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

  .btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
