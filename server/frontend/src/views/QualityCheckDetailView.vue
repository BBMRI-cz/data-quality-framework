<template>
  <div class="container-fluid px-2 px-md-3 py-3 py-md-4">
    <div class="row">
      <div class="col-12">
        <!-- Page Header -->
        <PageHeader
          :title="qualityCheck?.name || 'Quality Check Details'"
          subtitle="Quality Check Details"
          icon="bi bi-check-square"
        />

        <!-- Back Button -->
        <div class="mb-3">
          <button class="btn btn-outline-secondary btn-sm" @click="goBack">
            <i class="bi bi-arrow-left me-2"></i>Back to Quality Checks
          </button>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="loading-state">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading quality check...</span>
          </div>
        </div>

        <!-- Error State -->
        <div v-else-if="error" class="alert alert-danger" role="alert">
          <h6 class="alert-heading">Error Loading Quality Check</h6>
          <p class="mb-0">{{ error }}</p>
        </div>

        <!-- Detail View -->
        <div v-else-if="qualityCheck">
          <!-- Metadata Stat Card -->
          <div class="mb-4">
            <StatsCard
              label="Registered At"
              :value="formatDate(qualityCheck.registeredAt)"
              icon="bi bi-calendar-event"
              icon-color="#0d6efd"
              icon-bg-color="#cfe2ff"
              trend-text="Quality check creation date"
              trend-type="neutral"
            />
          </div>

          <!-- Edit Form Card -->
          <div class="card border-0 shadow-sm mb-4">
            <div class="card-header bg-white border-bottom py-3">
              <h5 class="mb-0 fw-semibold">
                <i class="bi bi-pencil text-primary me-2"></i>
                Edit Quality Check
              </h5>
            </div>
            <div class="card-body p-4">
              <div class="row g-4">
                <!-- Name Field -->
                <div class="col-12">
                  <label class="form-label fw-semibold">Name</label>
                  <input
                    v-model="editForm.name"
                    type="text"
                    class="form-control"
                    :class="{ 'is-invalid': validationErrors.name }"
                    placeholder="Enter check name"
                  />
                  <div v-if="validationErrors.name" class="invalid-feedback">
                    {{ validationErrors.name }}
                  </div>
                </div>

                <!-- Description Field -->
                <div class="col-12">
                  <label class="form-label fw-semibold">Description</label>
                  <textarea
                    v-model="editForm.description"
                    class="form-control"
                    rows="3"
                    placeholder="Enter check description"
                  ></textarea>
                </div>

                <!-- Category Field -->
                <div class="col-12">
                  <label class="form-label fw-semibold">Category</label>
                  <select v-model="editForm.categoryId" class="form-select">
                    <option :value="null">No Category</option>
                    <option v-for="category in categories" :key="category.id" :value="category.id">
                      {{ category.name }}
                    </option>
                  </select>
                </div>

                <!-- Warning Threshold -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold">
                    <i class="bi bi-exclamation-triangle text-warning me-1"></i>
                    Warning Threshold
                  </label>
                  <div class="input-group">
                    <input
                      v-model.number="editForm.warningThreshold"
                      type="number"
                      step="1"
                      min="0"
                      max="100"
                      class="form-control"
                      :class="{ 'is-invalid': validationErrors.warningThreshold }"
                    />
                    <span class="input-group-text">%</span>
                  </div>
                  <div v-if="validationErrors.warningThreshold" class="invalid-feedback d-block">
                    {{ validationErrors.warningThreshold }}
                  </div>
                  <small class="form-text text-muted">
                    Value between 0 and 100 (e.g., 5 for 5%)
                  </small>
                </div>

                <!-- Error Threshold -->
                <div class="col-md-6">
                  <label class="form-label fw-semibold">
                    <i class="bi bi-x-circle text-danger me-1"></i>
                    Error Threshold
                  </label>
                  <div class="input-group">
                    <input
                      v-model.number="editForm.errorThreshold"
                      type="number"
                      step="1"
                      min="0"
                      max="100"
                      class="form-control"
                      :class="{ 'is-invalid': validationErrors.errorThreshold }"
                    />
                    <span class="input-group-text">%</span>
                  </div>
                  <div v-if="validationErrors.errorThreshold" class="invalid-feedback d-block">
                    {{ validationErrors.errorThreshold }}
                  </div>
                  <small class="form-text text-muted">
                    Value between 0 and 100 (e.g., 10 for 10%)
                  </small>
                </div>

                <!-- Hash (Read-only) -->
                <div class="col-12">
                  <label class="form-label fw-semibold">Hash</label>
                  <div class="input-group">
                    <input
                      :value="qualityCheck.hash"
                      type="text"
                      class="form-control font-monospace bg-light"
                      readonly
                    />
                    <button
                      class="btn btn-outline-secondary"
                      type="button"
                      title="Copy to clipboard"
                      @click="copyHash"
                    >
                      <i class="bi bi-clipboard"></i>
                    </button>
                  </div>
                  <small class="form-text text-muted">
                    Unique identifier for this quality check
                  </small>
                </div>

                <!-- Keywords Section -->
                <div class="col-12">
                  <label class="form-label fw-semibold"> Keywords </label>
                  <div class="keywords-container">
                    <Badge
                      v-for="keyword in qualityCheck.keywords"
                      :key="keyword"
                      :text="keyword"
                      variant="primary"
                    />
                  </div>
                  <div
                    v-if="!qualityCheck.keywords || qualityCheck.keywords.length === 0"
                    class="text-muted"
                  >
                    <i class="bi bi-inbox me-1"></i>No keywords added yet
                  </div>
                  <small class="form-text text-muted">
                    Keywords help with NLP-based search and filtering
                  </small>
                </div>
              </div>
            </div>
          </div>

          <!-- Action Buttons -->
          <div class="action-buttons d-flex gap-3 justify-content-center">
            <button
              class="btn btn-action btn-save"
              :disabled="saving || !hasChanges"
              @click="saveCheck"
            >
              <span
                v-if="saving"
                class="spinner-border spinner-border-sm me-2"
                role="status"
              ></span>
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
  </div>
</template>

<script setup>
  import { ref, reactive, computed, onMounted } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { useHead } from '@unhead/vue';
  import { apiService } from '../services/apiService.js';
  import { notificationService } from '../services/notificationService.js';
  import { formatDateLong } from '../utils/dateUtils.js';
  import PageHeader from '../components/PageHeader.vue';
  import StatsCard from '../components/StatsCard.vue';
  import Badge from '../components/Badge.vue';

  const route = useRoute();
  const router = useRouter();

  const hash = ref(route.params.hash);
  const qualityCheck = ref(null);

  useHead({
    title: computed(() => qualityCheck.value?.name || 'Quality Check Details'),
  });
  const categories = ref([]);
  const loading = ref(true);
  const saving = ref(false);
  const error = ref(null);

  const editForm = reactive({
    name: '',
    description: '',
    categoryId: null,
    warningThreshold: 0,
    errorThreshold: 0,
  });

  const validationErrors = reactive({
    name: '',
    warningThreshold: '',
    errorThreshold: '',
  });

  const hasChanges = computed(() => {
    if (!qualityCheck.value) return false;
    return (
      editForm.name !== qualityCheck.value.name ||
      editForm.description !== (qualityCheck.value.description || '') ||
      editForm.categoryId !== (qualityCheck.value.category?.id || null) ||
      editForm.warningThreshold !== qualityCheck.value.warningThreshold ||
      editForm.errorThreshold !== qualityCheck.value.errorThreshold
    );
  });

  const formatDate = formatDateLong;

  const loadQualityCheck = async () => {
    loading.value = true;
    error.value = null;

    try {
      const [checksData, categoriesData] = await Promise.all([
        apiService.getQualityChecks(),
        apiService.getCategories(),
      ]);

      const checks =
        checksData._embedded?.qualityChecks || (Array.isArray(checksData) ? checksData : []);
      categories.value =
        categoriesData._embedded?.categories ||
        (Array.isArray(categoriesData) ? categoriesData : []);

      qualityCheck.value = checks.find((check) => check.hash === hash.value);

      if (!qualityCheck.value) {
        error.value = 'Quality check not found';
        return;
      }

      // Initialize form
      editForm.name = qualityCheck.value.name || '';
      editForm.description = qualityCheck.value.description || '';
      editForm.categoryId = qualityCheck.value.category?.id || null;
      editForm.warningThreshold = qualityCheck.value.warningThreshold ?? 0;
      editForm.errorThreshold = qualityCheck.value.errorThreshold ?? 0;
    } catch (err) {
      error.value = err.message || 'Failed to load quality check';
      console.error('Error loading quality check:', err);
    } finally {
      loading.value = false;
    }
  };

  const validate = () => {
    validationErrors.name = '';
    validationErrors.warningThreshold = '';
    validationErrors.errorThreshold = '';

    let isValid = true;

    if (!editForm.name.trim()) {
      validationErrors.name = 'Name is required';
      isValid = false;
    }

    if (editForm.warningThreshold < 0 || editForm.warningThreshold > 100) {
      validationErrors.warningThreshold = 'Must be between 0 and 100';
      isValid = false;
    }

    if (editForm.errorThreshold < 0 || editForm.errorThreshold > 100) {
      validationErrors.errorThreshold = 'Must be between 0 and 100';
      isValid = false;
    }

    if (editForm.warningThreshold > editForm.errorThreshold) {
      validationErrors.warningThreshold = 'Warning threshold should be less than error threshold';
      isValid = false;
    }

    return isValid;
  };

  const saveCheck = async () => {
    if (!validate()) return;

    saving.value = true;
    error.value = null;

    try {
      await apiService.updateQualityCheck(hash.value, editForm);

      notificationService.success(
        'Quality Check Updated',
        'Your changes have been saved successfully'
      );

      // Reload the data to show updated information
      await loadQualityCheck();
    } catch (err) {
      error.value = err.message || 'Failed to update quality check';
      console.error('Error updating quality check:', err);
      notificationService.error('Update Failed', error.value);
    } finally {
      saving.value = false;
    }
  };

  const resetForm = () => {
    editForm.name = qualityCheck.value.name || '';
    editForm.description = qualityCheck.value.description || '';
    editForm.categoryId = qualityCheck.value.category?.id || null;
    editForm.warningThreshold = qualityCheck.value.warningThreshold ?? 0;
    editForm.errorThreshold = qualityCheck.value.errorThreshold ?? 0;

    validationErrors.name = '';
    validationErrors.warningThreshold = '';
    validationErrors.errorThreshold = '';

    notificationService.info('Form Reset', 'Changes have been discarded');
  };

  const copyHash = () => {
    navigator.clipboard.writeText(qualityCheck.value.hash);
    notificationService.success('Copied', 'Hash copied to clipboard');
  };

  const goBack = () => {
    router.push('/quality-checks');
  };

  onMounted(() => {
    loadQualityCheck();
  });
</script>

<style scoped>
  /* Loading State */
  .loading-state {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 4rem 0;
  }

  /* Cards */
  .card {
    border-radius: 12px;
    overflow: hidden;
  }

  .card-header {
    background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  }

  /* Form Controls */
  .form-control:focus {
    border-color: #0d6efd;
    box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.15);
  }

  .font-monospace {
    font-family: var(--font-mono), monospace;
    font-size: 0.875rem;
  }

  .bg-light {
    background-color: #f8f9fa !important;
  }

  /* Action Buttons */
  .action-buttons {
    padding: 1rem 0;
  }

  .btn-action {
    border: none;
    padding: 0.75rem 2rem;
    border-radius: 0.5rem;
    font-weight: 600;
    font-size: 0.95rem;
    transition: all 0.3s ease;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 160px;
  }

  .btn-save {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  }

  .btn-save:hover:not(:disabled) {
    background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
    color: white;
  }

  .btn-save:active:not(:disabled) {
    transform: translateY(0);
    box-shadow: 0 2px 6px rgba(102, 126, 234, 0.3);
  }

  .btn-save:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .btn-reset {
    background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
    color: #495057;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  .btn-reset:hover:not(:disabled) {
    background: linear-gradient(135deg, #e9ecef 0%, #f8f9fa 100%);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    color: #495057;
  }

  .btn-reset:active:not(:disabled) {
    transform: translateY(0);
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  }

  .btn-reset:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .btn-action i {
    font-size: 1.1rem;
  }

  /* Keywords */
  .keywords-container {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
  }

  /* Responsive */
  @media (max-width: 768px) {
    .card-body {
      padding: 1.25rem;
    }

    .action-buttons {
      flex-direction: column;
    }

    .btn-action {
      width: 100%;
      min-width: auto;
    }
  }
</style>
