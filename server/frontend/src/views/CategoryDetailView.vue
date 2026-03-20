<template>
  <div class="container-fluid px-2 px-md-3 py-3 py-md-4">
    <div class="row">
      <div class="col-12">
        <!-- Page Header -->
        <PageHeader
          :title="isNew ? 'New Category' : category?.name || 'Unnamed Category'"
          :subtitle="isNew ? 'Create a new category' : 'Category Details'"
          icon="bi bi-tags"
        />

        <!-- Back Button and Actions -->
        <div class="mb-4 d-flex gap-2 align-items-center">
          <button class="btn btn-outline-secondary btn-sm" @click="goBack">
            <i class="bi bi-arrow-left me-2"></i>Back to Categories
          </button>
          <button
            v-if="!isNew"
            class="btn btn-outline-danger btn-sm d-flex align-items-center"
            :disabled="saving"
            @click="showDeleteModal = true"
          >
            <i class="bi bi-trash me-2"></i>
            Delete Category
          </button>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="loading-state">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading category...</span>
          </div>
        </div>

        <!-- Error State -->
        <div v-else-if="error" class="alert alert-danger" role="alert">
          <h6 class="alert-heading">Error Loading Category</h6>
          <p class="mb-0">{{ error }}</p>
        </div>

        <!-- Detail View -->
        <div v-else-if="category || isNew">
          <!-- Edit Form Card -->
          <div class="card border-0 shadow-sm mb-4">
            <div class="card-header bg-white border-bottom py-3">
              <h5 class="mb-0 fw-semibold">
                <i class="bi bi-pencil text-primary me-2"></i>
                {{ isNew ? 'Create Category' : 'Edit Category' }}
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
                    placeholder="Enter category name"
                  />
                  <div v-if="validationErrors.name" class="invalid-feedback">
                    {{ validationErrors.name }}
                  </div>
                </div>

                <!-- Color Field -->
                <div class="col-12">
                  <label class="form-label fw-semibold">Color</label>
                  <div class="d-flex gap-3 align-items-center">
                    <input
                      v-model="editForm.colorHex"
                      type="color"
                      class="form-control form-control-color"
                      title="Choose your color"
                    />
                    <input
                      v-model="editForm.colorHex"
                      type="text"
                      class="form-control"
                      :class="{ 'is-invalid': validationErrors.colorHex }"
                      placeholder="#000000"
                      style="max-width: 150px"
                    />
                  </div>
                  <div v-if="validationErrors.colorHex" class="invalid-feedback d-block">
                    {{ validationErrors.colorHex }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Action Buttons -->
          <div class="action-buttons d-flex gap-3 justify-content-center">
            <button
              class="btn btn-action btn-save"
              :disabled="saving || (!isNew && !hasChanges)"
              @click="saveCategory"
            >
              <span
                v-if="saving"
                class="spinner-border spinner-border-sm me-2"
                role="status"
              ></span>
              <i v-else class="bi bi-check-lg me-2"></i>
              {{ saving ? 'Saving...' : isNew ? 'Create Category' : 'Save Changes' }}
            </button>
            <button
              class="btn btn-action btn-reset"
              :disabled="saving || (!isNew && !hasChanges)"
              @click="resetForm"
            >
              <i class="bi bi-x-circle me-2"></i>
              Reset
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <DeleteConfirmModal
      :show="showDeleteModal"
      title="Delete Category"
      :message="`Are you sure you want to delete the category '${category?.name}'?`"
      warning="All quality checks assigned to this category will be unassigned."
      confirm-text="Delete Category"
      :loading="deleting"
      @close="showDeleteModal = false"
      @confirm="deleteCategory"
    />
  </div>
</template>

<script setup>
  import { ref, reactive, computed, onMounted } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { useHead } from '@unhead/vue';
  import { apiService } from '@/services/apiService.js';
  import { notificationService } from '@/services/notificationService.js';
  import PageHeader from '@/components/PageHeader.vue';
  import DeleteConfirmModal from '@/components/DeleteConfirmModal.vue';

  const route = useRoute();
  const router = useRouter();

  const isNew = computed(() => route.path === '/categories/new');
  const categoryId = ref(route.params.id);
  const category = ref(null);

  useHead({
    title: computed(() => {
      if (isNew.value) return 'New Category';
      return category.value?.name ? category.value.name : 'Category Detail';
    }),
  });
  const loading = ref(!isNew.value);
  const saving = ref(false);
  const deleting = ref(false);
  const error = ref(null);
  const showDeleteModal = ref(false);

  const editForm = reactive({
    name: '',
    colorHex: '#000000',
  });

  const validationErrors = reactive({
    name: '',
    colorHex: '',
  });

  const hasChanges = computed(() => {
    if (isNew.value) return true;
    if (!category.value) return false;
    return (
      editForm.name !== category.value.name ||
      editForm.colorHex !== (category.value.colorHex || '#000000')
    );
  });

  const loadCategory = async () => {
    if (isNew.value) {
      loading.value = false;
      return;
    }

    loading.value = true;
    error.value = null;

    try {
      const data = await apiService.getCategory(categoryId.value);
      category.value = data;

      // Initialize form
      editForm.name = category.value.name || '';
      editForm.colorHex = category.value.colorHex || '#000000';
    } catch (err) {
      error.value = err.message || 'Failed to load category';
      console.error('Error loading category:', err);
    } finally {
      loading.value = false;
    }
  };

  const validate = () => {
    validationErrors.name = '';
    validationErrors.colorHex = '';

    let isValid = true;

    if (!editForm.name.trim()) {
      validationErrors.name = 'Name is required';
      isValid = false;
    }

    if (!/^#[0-9A-F]{6}$/i.test(editForm.colorHex)) {
      validationErrors.colorHex = 'Invalid hex color format';
      isValid = false;
    }

    return isValid;
  };

  const saveCategory = async () => {
    if (!validate()) return;

    saving.value = true;
    error.value = null;

    try {
      if (isNew.value) {
        await apiService.createCategory(editForm);
        notificationService.success(
          'Category Created',
          'New category has been created successfully'
        );
        router.push('/categories');
      } else {
        await apiService.updateCategory(categoryId.value, editForm);
        notificationService.success(
          'Category Updated',
          'Your changes have been saved successfully'
        );
        await loadCategory();
      }
    } catch (err) {
      error.value = err.message || 'Failed to save category';
      console.error('Error saving category:', err);
      notificationService.error('Save Failed', error.value);
    } finally {
      saving.value = false;
    }
  };

  const deleteCategory = async () => {
    deleting.value = true;
    try {
      await apiService.deleteCategory(categoryId.value);
      notificationService.success('Category Deleted', 'Category has been successfully deleted');
      router.push('/categories');
    } catch (err) {
      console.error('Error deleting category:', err);
      notificationService.error('Delete Failed', err.message || 'Failed to delete category');
      showDeleteModal.value = false;
    } finally {
      deleting.value = false;
    }
  };

  const resetForm = () => {
    if (isNew.value) {
      editForm.name = '';
      editForm.colorHex = '#000000';
    } else {
      editForm.name = category.value.name || '';
      editForm.colorHex = category.value.colorHex || '#000000';
    }

    validationErrors.name = '';
    validationErrors.colorHex = '';

    notificationService.info('Form Reset', 'Changes have been discarded');
  };

  const goBack = () => {
    router.push('/categories');
  };

  onMounted(() => {
    loadCategory();
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

  .form-control-color {
    width: 50px;
    height: 38px;
    padding: 0.25rem;
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
