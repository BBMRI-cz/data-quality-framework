<template>
  <div class="server-registration-card">
    <div class="section-header">
      <div>
        <h2 class="section-title">
          <i class="bi bi-server"></i>
          Register Server
        </h2>
        <p class="section-description">
          Register your first central server to start sending quality reports
        </p>
      </div>
    </div>

    <form class="server-form" @submit.prevent="handleSubmit">
      <FormField
        id="serverName"
        v-model="formData.name"
        label="Server Name"
        icon="bi-tag"
        placeholder="e.g., Production Central Server"
        help-text="A descriptive name for this server (max 255 characters)"
        required
        maxlength="255"
      />

      <FormField
        id="serverUrl"
        v-model="formData.url"
        type="url"
        label="Server URL"
        icon="bi-link-45deg"
        placeholder="https://central.example.com"
        help-text="The base URL of the central server (max 500 characters)"
        required
        maxlength="500"
      >
        <template #default>
          <input
            id="serverUrl"
            v-model="formData.url"
            type="url"
            class="form-control"
            placeholder="https://central.example.com"
            required
            maxlength="500"
          />
          <div v-if="urlValidation.validating" class="validation-indicator validating">
            <i class="bi bi-arrow-clockwise spinning"></i>
            <span>Checking server...</span>
          </div>
          <div
            v-else-if="urlValidation.checked && urlValidation.valid"
            class="validation-indicator valid"
          >
            <i class="bi bi-check-circle-fill"></i>
            <div class="validation-text">
              <span class="validation-title">Server Reachable</span>
              <span class="validation-version">Version {{ urlValidation.version }}</span>
            </div>
          </div>
          <div
            v-else-if="urlValidation.checked && !urlValidation.valid"
            class="validation-indicator invalid"
          >
            <i class="bi bi-x-circle-fill"></i>
            <span>{{ urlValidation.error }}</span>
          </div>
        </template>
      </FormField>

      <FormActions
        :loading="loading"
        :show-cancel-button="false"
        save-text="Register Server"
        save-icon="bi-server"
        submit-type="submit"
      >
        <template #right>
          <button
            type="button"
            :disabled="isTestingConnection || !formData.url"
            class="btn btn-test"
            @click="testConnection"
          >
            <i
              :class="{
                'bi bi-arrow-clockwise spinning': isTestingConnection,
                'bi bi-plug': !isTestingConnection,
              }"
            ></i>
            {{ isTestingConnection ? 'Testing...' : 'Test Connection' }}
          </button>
          <SaveButton
            type="submit"
            :loading="loading"
            icon="bi-server"
            :text="loading ? 'Registering...' : 'Register Server'"
          />
        </template>
      </FormActions>
    </form>
  </div>
</template>

<script setup>
  import { reactive, ref } from 'vue';
  import { validateServerUrl } from '@/api';
  import SaveButton from './SaveButton.vue';
  import { FormField, FormActions } from '@/components/forms';

  defineProps({
    loading: {
      type: Boolean,
      default: false,
    },
  });

  const emit = defineEmits(['submit']);

  const isTestingConnection = ref(false);

  const formData = reactive({
    name: '',
    url: '',
  });

  const urlValidation = reactive({
    validating: false,
    checked: false,
    valid: false,
    version: null,
    error: null,
  });

  async function validateUrl() {
    const url = formData.url.trim();

    // Reset validation if URL is empty or invalid
    if (!url || !isValidUrlFormat(url)) {
      resetValidation();
      return;
    }

    // Remove trailing slash before validation
    const normalizedUrl = url.replace(/\/+$/, '');

    urlValidation.validating = true;
    urlValidation.checked = false;

    try {
      const result = await validateServerUrl(normalizedUrl);
      urlValidation.validating = false;
      urlValidation.checked = true;
      urlValidation.valid = result.valid;
      urlValidation.version = result.version || null;
      urlValidation.error = result.error || 'Invalid server';
    } catch (error) {
      urlValidation.validating = false;
      urlValidation.checked = true;
      urlValidation.valid = false;
      urlValidation.error = 'Validation failed';
    }
  }

  async function testConnection() {
    const url = formData.url.trim();

    // Validate URL format before testing
    if (!url || !isValidUrlFormat(url)) {
      urlValidation.checked = true;
      urlValidation.valid = false;
      urlValidation.error = 'Please enter a valid URL';
      return;
    }

    // Remove trailing slash before validation
    const normalizedUrl = url.replace(/\/+$/, '');

    isTestingConnection.value = true;
    urlValidation.validating = true;
    urlValidation.checked = false;

    try {
      const result = await validateServerUrl(normalizedUrl);
      urlValidation.validating = false;
      urlValidation.checked = true;
      urlValidation.valid = result.valid;
      urlValidation.version = result.version || null;
      urlValidation.error = result.error || 'Invalid server';
    } catch (error) {
      urlValidation.validating = false;
      urlValidation.checked = true;
      urlValidation.valid = false;
      urlValidation.error = 'Validation failed';
    } finally {
      isTestingConnection.value = false;
    }
  }

  function isValidUrlFormat(url) {
    try {
      const urlObj = new URL(url);
      return urlObj.protocol === 'http:' || urlObj.protocol === 'https:';
    } catch {
      return false;
    }
  }

  function resetValidation() {
    urlValidation.validating = false;
    urlValidation.checked = false;
    urlValidation.valid = false;
    urlValidation.version = null;
    urlValidation.error = null;
  }

  function handleSubmit() {
    // Remove trailing slash before submitting
    const url = formData.url.trim().replace(/\/+$/, '');

    emit('submit', {
      name: formData.name.trim(),
      url: url,
    });
  }

  function clearForm() {
    formData.name = '';
    formData.url = '';
    resetValidation();
  }

  defineExpose({ clearForm });
</script>

<style scoped>
  .server-registration-card {
    background: var(--bg-card);
    border-radius: var(--radius-xl);
    box-shadow: var(--shadow-sm);
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

  .server-form {
    max-width: 600px;
  }

  /* Form control styles for custom slot content */
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

  /* Validation indicator styles */
  .validation-indicator {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    margin-top: var(--spacing-sm);
    padding: 0.75rem var(--spacing-md);
    border-radius: var(--radius-md);
    font-size: 0.875rem;
  }

  .validation-indicator.validating {
    color: var(--color-gray-600);
    background: var(--color-gray-100);
  }

  .validation-indicator.valid {
    color: #059669;
    background: #d1fae5;
  }

  .validation-indicator.valid i {
    font-size: 1.25rem;
    flex-shrink: 0;
  }

  .validation-indicator.invalid {
    color: var(--color-danger);
    background: #fee2e2;
  }

  .validation-indicator.invalid i {
    font-size: 1.1rem;
    flex-shrink: 0;
  }

  .validation-text {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
  }

  .validation-title {
    font-weight: 600;
    line-height: 1.2;
  }

  .validation-version {
    font-size: 0.8rem;
    color: var(--color-gray-600);
    font-weight: 400;
  }

  /* Test button styles */
  .btn-test {
    background: var(--color-gray-100);
    color: var(--color-gray-700);
    padding: var(--spacing-sm) var(--spacing-lg);
    font-size: 0.95rem;
    font-weight: 600;
    border: none;
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: all var(--transition-base);
    display: inline-flex;
    align-items: center;
    gap: var(--spacing-sm);
  }

  .btn-test:hover:not(:disabled) {
    background: var(--color-gray-200);
    transform: translateY(-1px);
  }

  .btn-test:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .spinning {
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }

  /* Override FormActions for this context */
  :deep(.form-actions) {
    background-color: transparent;
    border-radius: 0;
    padding: var(--spacing-lg) 0 0 0;
    border-top: 2px solid var(--color-gray-100);
    margin-top: var(--spacing-xl);
    justify-content: flex-start;
  }

  /* Responsive */
  @media (max-width: 768px) {
    .server-registration-card {
      padding: var(--spacing-lg);
    }

    .section-title {
      font-size: 1.35rem;
    }

    .server-form {
      max-width: 100%;
    }
  }

  @media (max-width: 576px) {
    .server-registration-card {
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
