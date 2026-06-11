<template>
  <div class="container-fluid py-3 py-md-4">
    <PageHeader title="General Settings" subtitle="Configure application-wide settings">
      <template #icon>
        <i class="bi bi-sliders fs-2 text-primary"></i>
      </template>
    </PageHeader>

    <div class="row justify-content-center">
      <div class="col-12 col-lg-8">
        <div class="card border-0 shadow-sm">
          <div class="card-body p-4">
            <h3 class="h5 fw-bold mb-3">
              <i class="bi bi-gear-fill me-2 text-primary"></i>
              Application Configuration
            </h3>
            <p class="text-muted mb-4">
              Configure general application settings that affect data retention and system behavior.
            </p>

            <div v-if="isInitializing" class="text-center py-5">
              <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
              </div>
              <p class="text-muted mt-3">Loading settings...</p>
            </div>

            <form v-else @submit.prevent="saveSettings">
              <div class="mb-3">
                <label for="reportRetention" class="form-label"> Report Retention </label>
                <input
                  id="reportRetention"
                  v-model.number="generalSettings.reportRetention"
                  type="number"
                  class="form-control"
                  placeholder="3"
                />
                <div class="form-text">
                  Maximum number of reports to retain per agent. Older reports will be removed.
                </div>
              </div>

              <div class="d-flex flex-wrap button-group">
                <button type="submit" class="btn btn-primary" :disabled="loading">
                  <span
                    v-if="loading"
                    class="spinner-border spinner-border-sm me-2"
                    role="status"
                  ></span>
                  {{ loading ? 'Saving...' : 'Save Settings' }}
                </button>
                <button
                  type="button"
                  class="btn btn-outline-secondary"
                  :disabled="loading"
                  @click="resetSettings"
                >
                  Reset
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
  import { ref, onMounted } from 'vue';
  import settingsStore from '@/stores/settingsStore';
  import { notificationService } from '@/services/notificationService';
  import PageHeader from '@/components/ui/PageHeader.vue';

  const { generalSettings, loading, error } = settingsStore;
  const isInitializing = ref(true);

  async function saveSettings() {
    await settingsStore.updateGeneralSettings();

    if (error.value) {
      notificationService.error('Error', error.value, { duration: 5000, autoClose: true });
    } else {
      notificationService.success(
        'Settings Saved',
        'General settings have been updated successfully.',
        { duration: 3000, autoClose: true }
      );
    }
  }

  async function resetSettings() {
    await settingsStore.fetchGeneralSettings();
  }

  onMounted(async () => {
    await settingsStore.fetchGeneralSettings();
    isInitializing.value = false;
  });
</script>

<style scoped>
  .form-label {
    font-weight: 500;
    margin-bottom: 0.5rem;
  }

  .card {
    border-radius: 12px;
  }

  .button-group {
    gap: 0.5rem;
  }

  .button-group > * {
    margin-bottom: 0.5rem;
  }

  @media (max-width: 768px) {
    .card-body {
      padding: 1.5rem !important;
    }
  }
</style>
