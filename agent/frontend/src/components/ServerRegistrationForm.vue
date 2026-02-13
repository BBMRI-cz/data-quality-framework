<template>
  <div class="server-registration-form">
    <div class="empty-state-header">
      <div class="empty-icon">
        <i class="bi bi-server"></i>
      </div>
      <h2 class="empty-title">No Server Registered</h2>
      <p class="empty-description">
        Register your first central server to start sending quality reports
      </p>
    </div>

    <form class="server-form" @submit.prevent="handleSubmit">
      <div class="form-group">
        <label for="serverName" class="form-label">
          <i class="bi bi-tag"></i>
          Server Name
        </label>
        <input
          id="serverName"
          v-model="formData.name"
          type="text"
          class="form-control"
          placeholder="e.g., Production Central Server"
          required
          maxlength="255"
        />
        <small class="form-help"> A descriptive name for this server (max 255 characters) </small>
      </div>

      <div class="form-group">
        <label for="serverUrl" class="form-label">
          <i class="bi bi-link-45deg"></i>
          Server URL
        </label>
        <input
          id="serverUrl"
          v-model="formData.url"
          type="url"
          class="form-control"
          placeholder="https://central.example.com"
          required
          maxlength="500"
          pattern="^https?://[^\s/$.?#].[^\s]*$"
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
        <small class="form-help"> The base URL of the central server (max 500 characters) </small>
      </div>

      <div class="form-actions">
        <SaveButton
          type="submit"
          :loading="loading"
          :text="loading ? 'Registering...' : 'Register Server'"
        />
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
      </div>
    </form>
  </div>
</template>

<script setup>
  import { reactive, ref } from 'vue';
  import { validateServerUrl } from '@/api';
  import SaveButton from './SaveButton.vue';

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
  .server-registration-form {
    background: var(--bg-card);
    border-radius: var(--radius-xl);
    box-shadow: var(--shadow-sm);
    padding: var(--spacing-2xl);
  }

  .empty-state-header {
    text-align: center;
    margin-bottom: var(--spacing-2xl);
    padding-bottom: var(--spacing-2xl);
    border-bottom: 2px solid var(--color-gray-100);
  }

  .empty-icon {
    width: 80px;
    height: 80px;
    margin: 0 auto var(--spacing-lg);
    background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
    border-radius: var(--radius-xl);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .empty-icon i {
    font-size: 2.5rem;
    color: white;
  }

  .empty-title {
    font-size: 1.75rem;
    font-weight: 700;
    color: var(--color-gray-800);
    margin: 0 0 var(--spacing-sm) 0;
  }

  .empty-description {
    font-size: 1rem;
    color: var(--color-gray-500);
    margin: 0;
    line-height: 1.6;
  }

  .server-form {
    max-width: 600px;
    margin: 0 auto;
  }

  .form-group {
    margin-bottom: 1.75rem;
  }

  .form-label {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    font-size: 0.95rem;
    font-weight: 600;
    color: var(--color-gray-700);
    margin-bottom: var(--spacing-sm);
  }

  .form-label i {
    color: var(--color-primary);
  }

  .form-control {
    width: 100%;
    padding: 0.875rem var(--spacing-md);
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

  .validation-indicator.validating i {
    animation: spin 1s linear infinite;
    font-size: 1.1rem;
  }

  .validation-indicator.valid {
    color: #059669;
    background: #d1fae5;
  }

  .validation-indicator.valid i {
    font-size: 1.25rem;
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

  .validation-indicator.invalid {
    color: #dc2626;
    background: #fee2e2;
  }

  .validation-indicator.invalid i {
    font-size: 1.1rem;
    flex-shrink: 0;
  }

  @keyframes spin {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
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
    display: flex;
    gap: var(--spacing-md);
    justify-content: center;
  }

  .btn {
    padding: 0.875rem var(--spacing-lg);
    font-size: 0.95rem;
    font-weight: 600;
    border: none;
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all var(--transition-base);
    display: inline-flex;
    align-items: center;
    gap: var(--spacing-sm);
  }

  .btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .btn-test {
    background: var(--color-gray-100);
    color: var(--color-gray-700);
  }

  .btn-test:hover:not(:disabled) {
    background: var(--color-gray-200);
    transform: translateY(-1px);
  }

  .btn-test i {
    font-size: 1rem;
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

  @media (max-width: 768px) {
    .validation-indicator {
      position: static;
      transform: none;
      margin-top: var(--spacing-sm);
      justify-content: center;
    }
  }
</style>
