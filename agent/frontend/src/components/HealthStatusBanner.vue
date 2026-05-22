<template>
  <div
    v-if="healthStore.healthStatus"
    :class="[
      'alert',
      'mb-4',
      'health-status-banner',
      healthStore.healthStatus.status === 'UP' ? 'alert-success' : 'alert-danger',
    ]"
    role="alert"
  >
    <div class="d-flex justify-content-between align-items-center">
      <div class="d-flex flex-column">
        <span class="d-flex align-items-center gap-2">
          <i
            :class="[
              'bi',
              'fs-5',
              healthStore.healthStatus.status === 'UP'
                ? 'bi-check-circle-fill'
                : 'bi-exclamation-triangle-fill',
            ]"
          ></i>
          <strong>{{ dataSourceLabel }}:</strong> {{ healthStore.healthStatus.status }}
          <span v-if="healthStore.healthStatus.details?.error" class="ms-2">
            - {{ healthStore.healthStatus.details.error }}</span
          >
        </span>
        <small v-if="healthStore.healthStatus.status !== 'UP'" class="mt-2 tip-text">
          <i class="bi bi-info-circle me-1"></i>
          Tip: Check your {{ dataSourceLabel }} connection details in
          <router-link to="/settings" class="settings-link">Settings</router-link>
        </small>
      </div>
      <button
        class="btn btn-sm"
        :class="
          healthStore.healthStatus.status === 'UP' ? 'btn-outline-success' : 'btn-outline-danger'
        "
        :disabled="healthStore.isChecking"
        @click="healthStore.checkHealth()"
      >
        <span v-if="healthStore.isChecking" class="spinner-border spinner-border-sm me-1"></span>
        <i v-else class="bi bi-arrow-clockwise me-1"></i>
        Refresh
      </button>
    </div>
  </div>
</template>

<script setup>
  import { computed } from 'vue';
  import { useHealthStore } from '@/stores/healthStore.js';
  import { useSettingsStore } from '@/stores/settingsStore.js';

  const healthStore = useHealthStore();
  const settingsStore = useSettingsStore();

  const isSql = computed(() => settingsStore.settings?.databaseType === 'SQL');

  const dataSourceLabel = computed(() => (isSql.value ? 'SQL Database' : 'FHIR® Server'));
</script>

<style scoped>
  .health-status-banner {
    border-radius: var(--radius-md);
    border-left-width: 4px;
    font-size: 0.95rem;
    padding: 1rem 1.5rem;
    box-shadow: var(--shadow-sm);
  }

  .alert-success {
    background-color: #d1e7dd;
    border-color: #badbcc;
    border-left-color: var(--color-success);
    color: #0f5132;
  }

  .alert-danger {
    background-color: #f8d7da;
    border-color: #f5c2c7;
    border-left-color: var(--color-danger);
    color: #842029;
  }

  .tip-text {
    margin-left: 2rem;
    opacity: 0.9;
  }

  .settings-link {
    color: inherit;
    font-weight: 600;
    text-decoration: underline;
  }

  .settings-link:hover {
    opacity: 0.8;
  }

  @media (max-width: 768px) {
    .health-status-banner {
      padding: 0.875rem 1rem;
      font-size: 0.875rem;
    }

    .d-flex.gap-2 {
      gap: 0.5rem !important;
    }

    .d-flex > strong {
      display: none;
    }

    .tip-text {
      margin-left: 1.5rem;
      font-size: 0.813rem;
    }
  }
</style>
