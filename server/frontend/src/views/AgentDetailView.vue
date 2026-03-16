<template>
  <div class="container-fluid px-2 px-md-3 py-3 py-md-4">
    <div class="row">
      <div class="col-12">
        <!-- Page Header -->
        <PageHeader
          :title="agent?.name || 'Agent Details'"
          subtitle="Agent Details"
          icon="bi bi-database-fill-gear"
        />

        <!-- Back Button -->
        <div class="mb-4">
          <button class="btn btn-outline-secondary btn-sm" @click="goBack">
            <i class="bi bi-arrow-left me-2"></i>Back to Agent Reports
          </button>
        </div>

        <!-- Loading state -->
        <div v-if="loading" class="loading-state">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading agent...</span>
          </div>
        </div>

        <!-- Error state -->
        <div v-else-if="error" class="alert alert-danger" role="alert">
          <h6 class="alert-heading">Error Loading Agent</h6>
          <p class="mb-0">{{ error }}</p>
        </div>

        <!-- Agent Form -->
        <div v-else-if="agent" class="card border-0 shadow-sm mb-4">
          <div class="card-header bg-white border-bottom py-3">
            <h5 class="mb-0 fw-semibold">
              <i class="bi bi-pencil text-primary me-2"></i>
              Edit Agent
            </h5>
          </div>
          <div class="card-body p-4">
            <div class="row g-4">
              <!-- Agent ID Display -->
              <div class="col-md-6">
                <label for="agentId" class="form-label fw-semibold">Agent ID</label>
                <input
                  id="agentId"
                  :value="agent.id"
                  type="text"
                  class="form-control bg-light"
                  disabled
                />
                <small class="text-muted">Unique identifier for the agent</small>
              </div>

              <!-- Status Display -->
              <div class="col-md-6">
                <label class="form-label fw-semibold">Status</label>
                <div>
                  <span :class="getStatusClass(agent.status)" class="badge rounded-pill">
                    {{ agent.status }}
                  </span>
                </div>
              </div>

              <!-- Name Field -->
              <div class="col-md-6">
                <label for="name" class="form-label fw-semibold"> Name </label>
                <input
                  id="name"
                  v-model="form.name"
                  type="text"
                  class="form-control"
                  :class="{ 'is-invalid': validationErrors.name }"
                  placeholder="Enter agent name"
                />
                <div v-if="validationErrors.name" class="invalid-feedback">
                  {{ validationErrors.name }}
                </div>
              </div>

              <!-- External Identifier Field -->
              <div class="col-md-6">
                <label for="externalIdentifier" class="form-label fw-semibold">
                  External Identifier
                </label>
                <input
                  id="externalIdentifier"
                  v-model="form.externalIdentifier"
                  type="text"
                  class="form-control"
                  placeholder="Enter external identifier (optional)"
                />
                <small class="text-muted">Optional: External identifier for the agent</small>
              </div>

              <!-- Version Display -->
              <div v-if="agent.version" class="col-md-6">
                <label for="version" class="form-label fw-semibold"> Version </label>
                <input
                  id="version"
                  :value="agent.version"
                  type="text"
                  class="form-control bg-light"
                  disabled
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="action-buttons d-flex gap-3 justify-content-center">
          <button
            class="btn btn-action btn-save"
            :disabled="saving || !hasChanges"
            @click="saveAgent"
          >
            <span v-if="saving" class="spinner-border spinner-border-sm me-2" role="status"></span>
            <i v-else class="bi bi-check-lg me-2"></i>
            {{ saving ? 'Saving...' : 'Save Changes' }}
          </button>
          <button
            class="btn btn-action btn-reset"
            :disabled="saving || !hasChanges"
            @click="resetForm"
          >
            <i class="bi bi-x-circle me-2"></i>
            Reset
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted, reactive } from 'vue';
  import { useRouter, useRoute } from 'vue-router';
  import { apiService } from '../services/apiService.js';
  import { notificationService } from '../services/notificationService.js';
  import PageHeader from '../components/PageHeader.vue';

  const router = useRouter();
  const route = useRoute();

  const agentId = ref(route.params.id);

  const agent = ref(null);
  const loading = ref(false);
  const saving = ref(false);
  const error = ref(null);

  const form = reactive({
    name: '',
    externalIdentifier: '',
  });

  const validationErrors = reactive({
    name: '',
  });

  const hasChanges = computed(() => {
    if (!agent.value) return false;

    const nameChanged = form.name !== (agent.value.name || '');
    const externalIdentifierChanged =
      form.externalIdentifier !== (agent.value.externalIdentifier || '');

    return nameChanged || externalIdentifierChanged;
  });

  const loadAgent = async () => {
    loading.value = true;
    error.value = null;

    try {
      const data = await apiService.getAgent(agentId.value);
      agent.value = data;
      form.name = data.name || '';
      form.externalIdentifier = data.externalIdentifier || '';
    } catch (err) {
      error.value = err.message || 'Failed to load agent';
      console.error('Error loading agent:', err);
    } finally {
      loading.value = false;
    }
  };

  const saveAgent = async () => {
    saving.value = true;

    try {
      const agentData = {
        name: form.name.trim() || null,
        externalIdentifier: form.externalIdentifier.trim() || null,
      };

      const updated = await apiService.updateAgent(agentId.value, agentData);
      agent.value = updated;

      notificationService.success(
        'Agent Updated',
        `Agent "${agentId.value}" has been updated successfully.`
      );
    } catch (err) {
      console.error('Error saving agent:', err);
      notificationService.error(
        'Update Failed',
        err.message || 'Failed to update agent. Please try again.'
      );
    } finally {
      saving.value = false;
    }
  };

  const resetForm = () => {
    if (agent.value) {
      form.name = agent.value.name || '';
      form.externalIdentifier = agent.value.externalIdentifier || '';
    }
    validationErrors.name = '';
  };

  const getStatusClass = (status) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-success';
      case 'PENDING':
        return 'bg-warning text-dark';
      case 'INACTIVE':
        return 'bg-secondary';
      case 'ERROR':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  };

  const goBack = () => {
    router.push(`/agents/${agentId.value}/reports`);
  };

  onMounted(() => {
    loadAgent();
  });
</script>

<style scoped>
  /* Loading State */
  .loading-state {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 300px;
  }

  .spinner-border {
    width: 3rem;
    height: 3rem;
  }

  /* Card Styling */
  .card {
    border-radius: 12px;
    overflow: hidden;
  }

  /* Form Styling */
  .form-label {
    font-size: 0.875rem;
    font-weight: 500;
    margin-bottom: 0.5rem;
    color: var(--vt-c-text-light-1);
  }

  .form-control:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 0.2rem rgba(102, 126, 234, 0.25);
  }

  /* Action Buttons */
  .action-buttons {
    padding: 1.5rem 0;
  }

  .btn-action {
    border: none;
    padding: 0.75rem 2rem;
    border-radius: var(--radius-md);
    font-weight: 600;
    font-size: 0.95rem;
    transition: var(--transition-slow);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 160px;
  }

  .btn-save {
    background: var(--gradient-primary);
    color: white;
    box-shadow: var(--shadow-primary);
  }

  .btn-save:hover:not(:disabled) {
    background: var(--gradient-primary-reverse);
    transform: translateY(-2px);
    box-shadow: var(--shadow-primary-hover);
    color: white;
  }

  .btn-save:active:not(:disabled) {
    transform: translateY(0);
    box-shadow: var(--shadow-md);
  }

  .btn-save:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none !important;
  }

  .btn-reset {
    background: var(--color-gray-100);
    color: var(--color-gray-700);
    border: 1px solid var(--color-gray-300);
  }

  .btn-reset:hover:not(:disabled) {
    background: var(--color-gray-200);
    color: var(--color-gray-800);
    transform: translateY(-2px);
  }

  .btn-reset:active:not(:disabled) {
    transform: translateY(0);
  }

  .btn-reset:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none !important;
  }

  /* Badge styling */
  .badge {
    font-weight: 500;
    padding: 0.5rem 0.75rem;
    font-size: 0.813rem;
  }

  /* Responsive */
  @media (max-width: 768px) {
    .card-body {
      padding: 1rem !important;
    }

    .form-label {
      font-size: 0.813rem;
    }

    .action-buttons {
      flex-direction: column;
      gap: 1rem;
    }

    .btn-action {
      width: 100%;
      min-width: auto;
    }
  }
</style>
