<template>
  <div class="privacy-page">
    <PageHeader
      title="Differential Privacy"
      mobile-title="Privacy"
      subtitle="Configure privacy parameters for differential privacy protection"
      icon="bi bi-gear"
    />

    <div class="page-content">
      <div class="settings-card-container">
        <div class="settings-card">
          <div class="settings-section">
            <div class="section-header">
              <div>
                <h2 class="section-title">
                  <i class="bi bi-person-fill-lock"></i>
                  Privacy Parameters
                </h2>
                <p class="text-muted mb-0">
                  Configure the differential privacy settings to balance data utility and privacy
                  protection
                </p>
                <a
                  class="docs-link"
                  href="https://fdqf.bbmri-eric.eu/user/privacy-configuration"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Documentation
                  <i class="bi bi-box-arrow-up-right"></i>
                </a>
              </div>
            </div>

            <form class="settings-form" @submit.prevent="savePrivacySettings">
              <FormField
                id="epsilon"
                v-model.number="privacySettings.epsilon"
                type="number"
                step="0.1"
                min="0.1"
                label="Epsilon (ε)"
                icon="bi-graph-up"
                placeholder="1.0"
                help-text="Privacy budget - smaller values provide stronger privacy guarantees but add more noise to results"
                required
              />

              <FormField
                id="delta"
                v-model="privacySettings.delta"
                type="text"
                label="Delta (δ)"
                icon="bi-activity"
                placeholder="1e-8"
                help-text="Delta parameter - probability of privacy violation, typically set to very small values like 1e-8"
                required
              />

              <FormField
                id="minThreshold"
                v-model.number="privacySettings.minThreshold"
                type="number"
                min="0"
                label="Minimum Threshold"
                icon="bi-speedometer2"
                placeholder="10"
                help-text="Minimum count threshold for low count suppression - values below this will be hidden"
                required
              />

              <FormSelect
                id="noiseMechanism"
                v-model="privacySettings.noiseMechanism"
                label="Noise Mechanism"
                icon="bi-gear"
                help-text="Choose between Laplace (pure ε-DP) or Gaussian ((ε,δ)-DP) noise distribution"
                required
                :options="[
                  { value: 'LAPLACE', label: 'Laplace' },
                  { value: 'GAUSSIAN', label: 'Gaussian' },
                ]"
              />

              <div class="form-actions-wrapper">
                <FormActions
                  :loading="isSaving"
                  :show-cancel-button="false"
                  :save-text="isSaving ? 'Saving...' : 'Save Changes'"
                  save-icon="bi-check-circle"
                />
                <button
                  type="button"
                  class="btn btn-cancel"
                  :disabled="isSaving"
                  @click="resetSettings"
                >
                  <i class="bi bi-arrow-counterclockwise me-2"></i>
                  Discard Changes
                </button>
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
  import { useSettingsStore } from '@/stores/settingsStore.js';
  import PageHeader from '@/components/PageHeader.vue';
  import { FormField, FormActions, FormSelect } from '@/components/forms';
  import { notificationService } from '@/services/notificationService.js';

  const settingsStore = useSettingsStore();

  const isSaving = ref(false);

  const privacySettings = reactive({
    epsilon: 1.0,
    delta: '1e-8',
    minThreshold: 10,
    noiseMechanism: 'LAPLACE',
  });

  async function savePrivacySettings() {
    isSaving.value = true;

    try {
      const parsedDelta = Number(privacySettings.delta);
      if (!Number.isFinite(parsedDelta) || parsedDelta <= 0) {
        notificationService.error(
          'Invalid Delta',
          'Delta must be a positive number (e.g., 1e-8). Please correct the value and try again.'
        );
        isSaving.value = false;
        return;
      }

      const current = await settingsStore.fetchSettings();
      const payload = {
        fhirUrl: current.fhirUrl,
        fhirUsername: current.fhirUsername,
        fhirPassword: current.fhirPassword,
        epsilon: privacySettings.epsilon,
        delta: parsedDelta,
        minThreshold: privacySettings.minThreshold,
        noiseMechanism: privacySettings.noiseMechanism,
      };

      await settingsStore.updateSettings(payload);
      notificationService.success(
        'Privacy Settings Saved',
        'Your differential privacy settings have been updated successfully'
      );
    } catch (error) {
      console.error('Error saving privacy settings:', error);
      const apiDetail = error.response?.data?.detail;
      notificationService.error(
        'Save Failed',
        apiDetail || 'Unable to save privacy settings. Please try again.'
      );
    } finally {
      isSaving.value = false;
    }
  }

  async function loadSettings() {
    try {
      const data = await settingsStore.fetchSettings();
      if (data) {
        privacySettings.epsilon = data.epsilon ?? 3.0;
        privacySettings.delta = data.delta != null ? String(data.delta) : '1e-8';
        privacySettings.minThreshold = data.minThreshold ?? 50;
        privacySettings.noiseMechanism = data.noiseMechanism ?? 'LAPLACE';
      }
      return true;
    } catch (error) {
      console.error('Error loading privacy settings:', error);
      return false;
    }
  }

  async function resetSettings() {
    const success = await loadSettings();
    if (success) {
      notificationService.info(
        'Changes Discarded',
        'Settings have been reset to the last saved values'
      );
    } else {
      notificationService.error(
        'Reset Failed',
        'Unable to reload privacy settings. Your local changes were not discarded.'
      );
    }
  }

  onMounted(() => {
    loadSettings();
  });
</script>

<style scoped>
  .privacy-page {
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

  .form-actions-wrapper {
    display: flex;
    gap: var(--spacing-md);
    align-items: center;
    padding: var(--spacing-lg) 0 0 0;
    border-top: 2px solid var(--color-gray-100);
    margin-top: var(--spacing-xl);
  }

  .btn-cancel {
    background: var(--color-gray-500);
    border-color: var(--color-gray-500);
    color: white;
    border: none;
    padding: var(--spacing-sm) var(--spacing-lg);
    border-radius: var(--radius-sm);
    font-weight: 500;
    transition: all var(--transition-base);
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .btn-cancel:hover:not(:disabled) {
    background: var(--color-gray-600);
    border-color: var(--color-gray-600);
    transform: translateY(-1px);
    color: white;
  }

  .btn-cancel:active:not(:disabled) {
    transform: translateY(0);
  }

  .btn-cancel:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .btn-cancel:focus {
    outline: 2px solid rgba(107, 114, 128, 0.5);
    outline-offset: 2px;
  }

  :deep(.form-actions) {
    background-color: transparent;
    border-radius: 0;
    padding: 0;
    border: none;
    margin: 0;
    justify-content: flex-start;
  }

  @media (max-width: 768px) {
    .privacy-page {
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

    .form-actions-wrapper {
      flex-direction: column;
      align-items: stretch;
    }

    .btn-cancel {
      width: 100%;
      justify-content: center;
    }
  }

  @media (max-width: 576px) {
    .privacy-page {
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
