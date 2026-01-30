<template>
  <div class="quality-check-page">
    <PageHeader
      :title="isEditing ? 'Edit Quality Check' : 'Add Quality Check'"
      :mobileTitle="isEditing ? 'Edit Check' : 'Add Check'"
      :subtitle="isEditing ? 'Update check configuration' : 'Create a new quality check'"
      icon="bi bi-check2-square"
    />

    <div class="page-content">
      <div class="form-container">
        <form @submit.prevent="handleSave" class="quality-check-form">
          <!-- Basic Information Section -->
          <div class="form-section">
            <h6 class="section-title">
              <i class="bi bi-info-circle me-2"></i>
              Basic Information
            </h6>
            <div class="mb-3">
              <label for="checkName" class="form-label fw-semibold">
                Name <span class="text-danger">*</span>
              </label>
              <input
                v-model="formData.name"
                type="text"
                class="form-control form-control-lg"
                id="checkName"
                placeholder="Enter check name"
                :class="{ 'is-invalid': errors.name }"
                required
              />
              <div v-if="errors.name" class="invalid-feedback">
                {{ errors.name }}
              </div>
            </div>
            <div class="mb-3">
              <label for="checkDescription" class="form-label fw-semibold">Description</label>
              <textarea
                v-model="formData.description"
                class="form-control"
                id="checkDescription"
                rows="2"
                placeholder="Enter a brief description of what this check validates"
              ></textarea>
              <small class="form-text text-muted">Optional: Helps others understand the purpose of this check</small>
            </div>
          </div>

          <!-- Query Configuration Section -->
          <div class="form-section">
            <h6 class="section-title">
              <i class="bi bi-code-square me-2"></i>
              Query Configuration
            </h6>
            <div class="mb-3">
              <label for="checkQuery" class="form-label fw-semibold">
                Query <span class="text-danger">*</span>
              </label>
              <textarea
                v-model="formData.query"
                class="form-control font-monospace query-input"
                id="checkQuery"
                rows="8"
                placeholder="Enter your CQL query here..."
                :class="{ 'is-invalid': errors.query }"
                required
              ></textarea>
              <div v-if="errors.query" class="invalid-feedback">
                {{ errors.query }}
              </div>
              <small class="form-text text-muted">
                <i class="bi bi-lightbulb me-1"></i>
                Write a CQL query to validate data quality
              </small>
            </div>
          </div>

          <!-- Thresholds & Budget Section -->
          <div class="form-section">
            <h6 class="section-title">
              <i class="bi bi-sliders me-2"></i>
              Thresholds & Budget
            </h6>
            <div class="row g-3">
              <div class="col-md-4">
                <label for="checkWarningThreshold" class="form-label fw-semibold">
                  <i class="bi bi-exclamation-triangle text-warning me-1"></i>
                  Warning Threshold
                </label>
                <input
                  v-model.number="formData.warningThreshold"
                  type="number"
                  class="form-control"
                  id="checkWarningThreshold"
                  placeholder="10"
                />
                <small class="form-text text-muted">Trigger warning at this value</small>
              </div>
              <div class="col-md-4">
                <label for="checkErrorThreshold" class="form-label fw-semibold">
                  <i class="bi bi-x-circle text-danger me-1"></i>
                  Error Threshold
                </label>
                <input
                  v-model.number="formData.errorThreshold"
                  type="number"
                  class="form-control"
                  id="checkErrorThreshold"
                  placeholder="30"
                />
                <small class="form-text text-muted">Trigger error at this value</small>
              </div>
              <div class="col-md-4">
                <label for="checkEpsilonBudget" class="form-label fw-semibold">
                  <i class="bi bi-shield-check text-info me-1"></i>
                  Epsilon Budget
                </label>
                <input
                  v-model.number="formData.epsilonBudget"
                  type="number"
                  step="0.1"
                  class="form-control"
                  id="checkEpsilonBudget"
                  placeholder="1.0"
                />
                <small class="form-text text-muted">Privacy budget allocation</small>
              </div>
            </div>
          </div>

          <!-- Form Actions -->
          <div class="form-actions">
            <button
              v-if="isEditing"
              type="button"
              class="btn btn-danger"
              @click="handleDelete"
              :disabled="saving"
            >
              <i class="bi bi-trash me-2"></i>
              Delete
            </button>
            <div class="form-actions-right">
              <button type="button" class="btn btn-secondary" @click="goBack">
                <i class="bi bi-arrow-left me-2"></i>
                Cancel
              </button>
              <SaveButton
                type="submit"
                :loading="saving"
                :text="saving ? (isEditing ? 'Updating...' : 'Creating...') : (isEditing ? 'Update Check' : 'Create Check')"
              />
            </div>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '@/js/api.js';
import PageHeader from '@/components/PageHeader.vue';
import SaveButton from '@/components/SaveButton.vue';

const route = useRoute();
const router = useRouter();

const formData = ref({
  id: null,
  name: '',
  description: '',
  query: '',
  warningThreshold: 10,
  errorThreshold: 30,
  epsilonBudget: 1.0
});

const errors = ref({});
const saving = ref(false);
const loading = ref(false);

const isEditing = computed(() => !!formData.value.id);

const validateForm = () => {
  const validationErrors = {};

  if (!formData.value.name?.trim()) {
    validationErrors.name = 'Name is required';
  }

  if (!formData.value.query?.trim()) {
    validationErrors.query = 'CQL query is required';
  }

  errors.value = validationErrors;
  return Object.keys(validationErrors).length === 0;
};

const loadCheck = async () => {
  if (route.params.id) {
    loading.value = true;
    try {
      const response = await api.get(`/api/quality-checks/${route.params.id}`);
      formData.value = response.data;
    } catch (error) {
      console.error('Failed to load check:', error);
      errors.value.general = 'Failed to load check';
    } finally {
      loading.value = false;
    }
  }
};

const handleSave = async () => {
  if (!validateForm()) {
    return;
  }

  saving.value = true;
  try {
    if (isEditing.value) {
      await api.put(`/api/quality-checks/${formData.value.id}`, formData.value);
    } else {
      await api.post('/api/quality-checks', formData.value);
    }
    router.push('/quality-checks');
  } catch (error) {
    console.error('Failed to save check:', error);
    errors.value.general = 'Failed to save check. Please try again.';
  } finally {
    saving.value = false;
  }
};

const goBack = () => {
  router.back();
};

const handleDelete = async () => {
  if (!confirm('Are you sure you want to delete this quality check? This action cannot be undone.')) {
    return;
  }

  saving.value = true;
  try {
    await api.delete(`/api/quality-checks/${formData.value.id}`);
    router.push('/quality-checks');
  } catch (error) {
    console.error('Failed to delete check:', error);
    errors.value.general = 'Failed to delete check. Please try again.';
  } finally {
    saving.value = false;
  }
};

onMounted(() => {
  loadCheck();
});
</script>

<style scoped>
.quality-check-page {
  min-height: 100%;
  padding: 2rem;
}

.page-content {
  width: 100%;
}

.form-container {
  max-width: 900px;
  margin: 0 auto;
}

.quality-check-form {
  background: white;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* Form Sections */
.form-section {
  padding: 2rem;
  border-bottom: 1px solid #e9ecef;
}

.form-section:last-of-type {
  border-bottom: none;
}

.section-title {
  color: #495057;
  font-size: 0.875rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 600;
  padding-bottom: 1rem;
  margin-bottom: 1.5rem;
  border-bottom: 2px solid #e9ecef;
}

/* Form Controls */
.form-label {
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  color: #495057;
}

.form-control {
  font-size: 0.875rem;
  border: 1px solid #dee2e6;
  transition: all 0.2s ease;
}

.form-control:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 0.2rem rgba(102, 126, 234, 0.25);
}

.form-control-lg {
  font-size: 1rem;
  padding: 0.75rem 1rem;
}

.form-text {
  display: block;
  margin-top: 0.25rem;
  font-size: 0.813rem;
}

.query-input {
  background-color: #f8f9fa;
  line-height: 1.6;
}

.font-monospace {
  font-family: 'SF Mono', 'Monaco', 'Courier New', monospace;
}

.invalid-feedback {
  display: block;
  width: 100%;
  margin-top: 0.25rem;
  font-size: 0.875rem;
  color: #dc3545;
}

.is-invalid {
  border-color: #dc3545 !important;
  background-color: #fff5f5;
}

.is-invalid:focus {
  box-shadow: 0 0 0 0.2rem rgba(220, 53, 69, 0.25);
}

/* Form Actions */
.form-actions {
  display: flex;
  gap: 1rem;
  padding: 2rem;
  border-top: 1px solid #e9ecef;
  background-color: #f8f9fa;
  border-radius: 0 0 0.5rem 0.5rem;
  justify-content: space-between;
  align-items: center;
}

.form-actions-right {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.form-actions .btn {
  min-width: 120px;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .quality-check-page {
    padding: 1rem;
  }

  .form-section {
    padding: 1.5rem;
  }

  .form-actions {
    flex-direction: column;
    padding: 1.5rem;
  }

  .form-actions-right {
    flex-direction: column;
    width: 100%;
  }

  .form-actions .btn {
    width: 100%;
  }
}

@media (max-width: 576px) {
  .quality-check-page {
    padding: 0.75rem;
  }

  .form-section {
    padding: 1rem;
  }

  .form-actions {
    padding: 1rem;
  }

  .row.g-3 {
    gap: 1rem !important;
  }

  .col-md-4 {
    width: 100%;
  }
}
</style>

