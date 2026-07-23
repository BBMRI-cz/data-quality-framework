<template>
  <div class="category-edit-page">
    <PageHeader
      :title="isNew ? 'New Category' : 'Edit Category'"
      :mobile-title="isNew ? 'New Category' : 'Edit'"
      :subtitle="isNew ? 'Create a new category' : 'Update category details'"
      icon="bi bi-tags"
    />

    <div class="page-content">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>Loading category...</p>
      </div>

      <div v-else-if="error" class="alert alert-danger" role="alert">
        <h6 class="alert-heading">Error Loading Category</h6>
        <p class="mb-0">{{ error }}</p>
      </div>

      <div v-else class="form-card">
        <div class="section-header">
          <h2 class="section-title">
            <i class="bi bi-pencil"></i>
            {{ isNew ? 'Create Category' : 'Edit Category' }}
          </h2>
        </div>

        <form class="category-form" @submit.prevent="onSave">
          <FormField
            id="categoryName"
            v-model="formData.name"
            label="Name"
            icon="bi-tag"
            placeholder="Enter category name"
            :error="errors.name"
            required
          />

          <div class="form-field" :class="{ 'has-error': errors.colorHex }">
            <label class="form-label">
              <i class="bi bi-palette"></i>
              <span>Color</span>
            </label>
            <div class="color-inputs">
              <input
                v-model="formData.colorHex"
                type="color"
                class="form-control form-control-color"
                title="Choose category color"
              />
              <input
                v-model="formData.colorHex"
                type="text"
                class="form-control color-text"
                :class="{ 'is-invalid': errors.colorHex }"
                placeholder="#000000"
                maxlength="7"
              />
            </div>
            <div v-if="errors.colorHex" class="invalid-feedback">
              {{ errors.colorHex }}
            </div>
          </div>

          <div class="preview-section">
            <label class="form-label">Preview</label>
            <CategoryBadge :category="previewCategory" />
          </div>

          <FormActions
            :loading="saving"
            :show-delete-button="!isNew"
            save-text="Save Category"
            delete-text="Delete Category"
            cancel-text="Cancel"
            save-icon="bi-check-circle"
            @cancel="goBack"
            @save="onSave"
            @delete="showDeleteModal = true"
          />
        </form>
      </div>
    </div>

    <BaseModal
      v-if="showDeleteModal"
      :show="true"
      :show-footer="false"
      @close="showDeleteModal = false"
    >
      <template #header>
        <div class="modal-header-content">
          <h3 class="mb-0">Delete Category</h3>
        </div>
      </template>

      <div class="modal-body-content">
        <p class="lead-text">
          Are you sure you want to delete the category <strong>{{ category?.name }}</strong
          >?
        </p>
        <div class="alert alert-warning">
          <i class="bi bi-exclamation-triangle me-2"></i>
          <div class="alert-content">
            Quality checks assigned to this category will be unassigned.
          </div>
        </div>
        <div class="modal-footer-custom">
          <button class="btn btn-secondary" :disabled="deleting" @click="showDeleteModal = false">
            Cancel
          </button>
          <button class="btn btn-danger" :disabled="deleting" @click="onDelete">
            <span v-if="deleting">
              <span
                class="spinner-border spinner-border-sm me-2"
                role="status"
                aria-hidden="true"
              ></span>
              Deleting...
            </span>
            <span v-else>
              <i class="bi bi-trash me-2"></i>
              Delete Category
            </span>
          </button>
        </div>
      </div>
    </BaseModal>
  </div>
</template>

<script setup>
  import { ref, computed, reactive, onMounted } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import PageHeader from '@/components/PageHeader.vue';
  import CategoryBadge from '@/components/CategoryBadge.vue';
  import BaseModal from '@/components/BaseModal.vue';
  import { FormField, FormActions } from '@/components/forms';
  import { categoryService } from '@/services/categoryService.js';
  import { notificationService } from '@/services/notificationService.js';

  const route = useRoute();
  const router = useRouter();

  const categoryId = computed(() => route.params.id);
  const isNew = computed(() => !categoryId.value);

  const category = ref(null);
  const loading = ref(false);
  const saving = ref(false);
  const deleting = ref(false);
  const error = ref(null);
  const showDeleteModal = ref(false);

  const formData = reactive({
    name: '',
    colorHex: '#6c757d',
  });

  const errors = reactive({
    name: '',
    colorHex: '',
  });

  const previewCategory = computed(() => ({
    name: formData.name || 'Preview',
    colorHex: formData.colorHex,
  }));

  const validate = () => {
    errors.name = '';
    errors.colorHex = '';
    let isValid = true;

    if (!formData.name.trim()) {
      errors.name = 'Name is required';
      isValid = false;
    }

    if (!/^#[0-9A-Fa-f]{6}$/.test(formData.colorHex)) {
      errors.colorHex = 'Invalid hex color format (e.g., #FF5733)';
      isValid = false;
    }

    return isValid;
  };

  const loadCategory = async () => {
    if (isNew.value) {
      loading.value = false;
      return;
    }

    loading.value = true;
    error.value = null;

    try {
      category.value = await categoryService.get(categoryId.value);
      formData.name = category.value.name || '';
      formData.colorHex = category.value.colorHex || '#6c757d';
    } catch (err) {
      error.value = err.response?.data?.message || 'Failed to load category';
      console.error('Failed to load category:', err);
    } finally {
      loading.value = false;
    }
  };

  const onSave = async () => {
    if (!validate()) return;

    saving.value = true;
    try {
      const payload = {
        name: formData.name.trim(),
        colorHex: formData.colorHex,
      };

      if (isNew.value) {
        await categoryService.create(payload);
        notificationService.success(
          'Category Created',
          'New category has been created successfully'
        );
      } else {
        await categoryService.update(categoryId.value, payload);
        notificationService.success('Category Updated', 'Category has been updated successfully');
      }
      router.push('/categories');
    } catch (err) {
      console.error('Failed to save category:', err);
      const message = err.response?.data?.message || 'Unable to save category. Please try again.';
      notificationService.error('Save Failed', message);
    } finally {
      saving.value = false;
    }
  };

  const onDelete = async () => {
    deleting.value = true;
    try {
      await categoryService.delete(categoryId.value);
      notificationService.success('Category Deleted', 'Category has been deleted successfully');
      router.push('/categories');
    } catch (err) {
      console.error('Failed to delete category:', err);
      const message = err.response?.data?.message || 'Unable to delete category. Please try again.';
      notificationService.error('Delete Failed', message);
    } finally {
      deleting.value = false;
      showDeleteModal.value = false;
    }
  };

  const goBack = () => {
    router.push('/categories');
  };

  onMounted(loadCategory);
</script>

<style scoped>
  .category-edit-page {
    min-height: 100%;
    padding: var(--spacing-xl);
  }

  .page-content {
    max-width: 700px;
    margin: 0 auto;
  }

  .form-card {
    background: var(--bg-card);
    border-radius: var(--radius-xl);
    box-shadow: var(--shadow-sm);
    padding: var(--spacing-2xl);
  }

  .section-header {
    margin-bottom: var(--spacing-xl);
  }

  .section-title {
    font-size: 1.5rem;
    font-weight: 700;
    color: var(--color-gray-800);
    margin: 0;
    display: flex;
    align-items: center;
    gap: 0.75rem;
  }

  .section-title i {
    color: var(--color-primary);
  }

  .category-form {
    max-width: 100%;
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

  .color-inputs {
    display: flex;
    align-items: center;
    gap: var(--spacing-md);
  }

  .form-control-color {
    width: 60px;
    height: 48px;
    padding: 0.25rem;
    cursor: pointer;
  }

  .color-text {
    max-width: 150px;
    font-family: var(--font-mono), monospace;
  }

  .invalid-feedback {
    display: block;
    margin-top: var(--spacing-xs);
    font-size: 0.875rem;
    color: var(--color-danger);
  }

  .preview-section {
    margin-bottom: var(--spacing-xl);
  }

  .loading-state {
    text-align: center;
    padding: var(--spacing-3xl);
    color: var(--color-gray-500);
  }

  .spinner {
    width: 2.5rem;
    height: 2.5rem;
    border: 3px solid var(--color-gray-200);
    border-top-color: var(--color-primary);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
    margin: 0 auto var(--spacing-md);
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }

  .alert {
    padding: 1rem;
    border-radius: var(--radius-md);
    margin-bottom: 1rem;
    display: flex;
    gap: 0.5rem;
  }

  .alert-danger {
    background-color: #fee2e2;
    border: 1px solid #fecaca;
    color: var(--color-danger);
  }

  .alert-warning {
    background-color: #fef3c7;
    border: 1px solid #fde68a;
    color: #92400e;
  }

  .alert-heading {
    margin: 0 0 var(--spacing-sm);
    font-weight: 600;
  }

  .modal-header-content {
    width: 100%;
  }

  .modal-body-content {
    padding: 0.5rem 0;
  }

  .modal-footer-custom {
    display: flex;
    gap: 0.75rem;
    justify-content: center;
    margin-top: 1.5rem;
    padding-top: 1rem;
    border-top: 1px solid var(--color-gray-200);
  }

  .lead-text {
    font-size: 1.1rem;
    margin-bottom: 1.5rem;
    color: var(--color-gray-800);
  }

  .alert-content {
    flex: 1;
  }

  .btn {
    padding: 0.75rem 1.5rem;
    font-size: 1rem;
    font-weight: 600;
    border: none;
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all var(--transition-base);
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .btn-secondary {
    background: var(--color-gray-200);
    color: var(--color-gray-700);
  }

  .btn-secondary:hover:not(:disabled) {
    background: var(--color-gray-300);
  }

  .btn-danger {
    background: var(--color-danger);
    color: white;
  }

  .btn-danger:hover:not(:disabled) {
    background: #b91c1c;
  }

  .spinner-border {
    width: 1rem;
    height: 1rem;
    border-width: 0.15em;
  }

  @media (max-width: 768px) {
    .category-edit-page {
      padding: var(--spacing-md);
    }

    .form-card {
      padding: var(--spacing-lg);
    }

    .section-title {
      font-size: 1.35rem;
    }
  }

  @media (max-width: 576px) {
    .category-edit-page {
      padding: var(--spacing-sm);
    }

    .form-card {
      padding: 1.25rem;
    }

    .color-inputs {
      gap: var(--spacing-sm);
    }

    .form-control-color {
      width: 52px;
      height: 44px;
    }

    .modal-footer-custom {
      flex-direction: column;
    }
  }
</style>
