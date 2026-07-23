<template>
  <div class="container-fluid px-2 px-md-3 py-3 py-md-4">
    <div class="row">
      <div class="col-12">
        <!-- Page Header -->
        <PageHeader
          :title="isNew ? 'New User' : user?.username || 'User Details'"
          :subtitle="isNew ? 'Create a new user' : 'User Details'"
          icon="bi bi-person"
        />

        <!-- Back Button and Actions -->
        <div class="mb-4 d-flex gap-2 align-items-center">
          <button class="btn btn-outline-secondary btn-sm" @click="goBack">
            <i class="bi bi-arrow-left me-2"></i>Back to Users
          </button>
          <button
            v-if="!isNew"
            class="btn btn-outline-danger btn-sm d-flex align-items-center"
            :disabled="saving"
            @click="showDeleteModal = true"
          >
            <i class="bi bi-trash me-2"></i>
            Delete User
          </button>
        </div>

        <!-- Loading state -->
        <div v-if="loading" class="loading-state">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading user...</span>
          </div>
        </div>

        <!-- Error state -->
        <div v-else-if="error" class="alert alert-danger" role="alert">
          <h6 class="alert-heading">Error Loading User</h6>
          <p class="mb-0">{{ error }}</p>
        </div>

        <!-- User Form -->
        <div v-else-if="user || isNew" class="card border-0 shadow-sm mb-4">
          <div class="card-header bg-white border-bottom py-3">
            <h5 class="mb-0 fw-semibold">
              <i class="bi bi-pencil text-primary me-2"></i>
              {{ isNew ? 'Create User' : 'Edit User' }}
            </h5>
          </div>
          <div class="card-body p-4">
            <div class="row g-4">
              <!-- Username Field -->
              <div class="col-md-6">
                <label for="username" class="form-label fw-semibold"> Username </label>
                <input
                  id="username"
                  v-model="form.username"
                  type="text"
                  class="form-control"
                  :class="{ 'is-invalid': validationErrors.username }"
                  placeholder="Enter username"
                  disabled
                />
                <div v-if="validationErrors.username" class="invalid-feedback">
                  {{ validationErrors.username }}
                </div>
              </div>

              <!-- Agent ID Field -->
              <div class="col-md-6">
                <label for="agentId" class="form-label fw-semibold"> Agent ID </label>
                <input
                  id="agentId"
                  v-model="form.agentId"
                  type="text"
                  class="form-control"
                  placeholder="Enter agent ID (optional)"
                  disabled
                />
                <small class="text-muted">Optional: Associate user with an agent</small>
              </div>

              <!-- Subject ID Display -->
              <div v-if="user?.subjectId" class="col-md-6">
                <label for="subjectId" class="form-label fw-semibold"> Subject ID </label>
                <input
                  id="subjectId"
                  :value="user.subjectId"
                  type="text"
                  class="form-control bg-light"
                  disabled
                />
                <small class="text-muted">Unique identifier from identity provider</small>
              </div>

              <!-- Roles Display -->
              <div v-if="user?.roles && user.roles.length > 0" class="col-12">
                <label class="form-label fw-semibold">Roles</label>
                <div>
                  <Badge v-for="role in user.roles" :key="role" :text="role" variant="primary" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="action-buttons d-flex gap-3 justify-content-center">
          <button
            class="btn btn-action btn-save"
            :disabled="saving || !isFormValid || (!isNew && !hasChanges)"
            @click="saveUser"
          >
            <span v-if="saving" class="spinner-border spinner-border-sm me-2" role="status"></span>
            <i v-else class="bi bi-check-lg me-2"></i>
            {{ saving ? 'Saving...' : isNew ? 'Create User' : 'Save Changes' }}
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

    <!-- Delete Confirmation Modal -->
    <DeleteConfirmModal
      :show="showDeleteModal"
      title="Delete User"
      :message="`Are you sure you want to delete the user '${user?.username}'?`"
      warning="This action cannot be undone."
      confirm-text="Delete User"
      :loading="deleting"
      @close="showDeleteModal = false"
      @confirm="deleteUser"
    />
  </div>
</template>

<script setup>
  import { ref, computed, onMounted, reactive } from 'vue';
  import { useRouter, useRoute } from 'vue-router';
  import { useHead } from '@unhead/vue';
  import { apiService } from '@/services/apiService.js';
  import { notificationService } from '@/services/notificationService.js';
  import PageHeader from '@/components/ui/PageHeader.vue';
  import DeleteConfirmModal from '@/components/DeleteConfirmModal.vue';
  import Badge from '@/components/ui/Badge.vue';

  const router = useRouter();
  const route = useRoute();

  const isNew = computed(() => route.path === '/users/new');
  const userId = ref(route.params.id);

  const user = ref(null);
  const loading = ref(false);
  const saving = ref(false);
  const deleting = ref(false);
  const error = ref(null);
  useHead({
    title: computed(() => {
      if (isNew.value) return 'New User';
      return user.value?.username ? user.value.username : 'User Detail';
    }),
  });
  const showDeleteModal = ref(false);

  const form = reactive({
    username: '',
    agentId: '',
  });

  const validationErrors = reactive({
    username: '',
  });

  const hasChanges = computed(() => {
    if (isNew.value) return true;
    if (!user.value) return false;

    const usernameChanged = form.username !== user.value.username;
    const agentIdChanged = (form.agentId || '') !== (user.value.agentId || '');

    return usernameChanged || agentIdChanged;
  });

  const isFormValid = computed(() => {
    return form.username.trim() !== '' && !saving.value;
  });

  const loadUser = async () => {
    if (isNew.value) {
      loading.value = false;
      return;
    }

    loading.value = true;
    error.value = null;

    try {
      const data = await apiService.getUser(userId.value);
      user.value = data;
      form.username = data.username || '';
      form.agentId = data.agentId || '';
    } catch (err) {
      error.value = err.message || 'Failed to load user';
      console.error('Error loading user:', err);
    } finally {
      loading.value = false;
    }
  };

  const validateForm = () => {
    validationErrors.username = '';

    if (!form.username.trim()) {
      validationErrors.username = 'Username is required';
      return false;
    }

    return true;
  };

  const saveUser = async () => {
    if (!validateForm()) {
      return;
    }

    saving.value = true;

    try {
      const userData = {
        username: form.username.trim(),
        agentId: form.agentId.trim() || null,
      };

      if (isNew.value) {
        const created = await apiService.createUser(userData);

        notificationService.success(
          'User Created',
          `User "${form.username}" has been created successfully.`
        );

        router.push(`/users/${created.id}`);
      } else {
        const updated = await apiService.updateUser(userId.value, userData);
        user.value = updated;

        notificationService.success(
          'User Updated',
          `User "${form.username}" has been updated successfully.`
        );
      }
    } catch (err) {
      console.error('Error saving user:', err);
      notificationService.error(
        isNew.value ? 'Create Failed' : 'Update Failed',
        err.message || `Failed to ${isNew.value ? 'create' : 'update'} user. Please try again.`
      );
    } finally {
      saving.value = false;
    }
  };

  const resetForm = () => {
    if (isNew.value) {
      form.username = '';
      form.agentId = '';
    } else if (user.value) {
      form.username = user.value.username || '';
      form.agentId = user.value.agentId || '';
    }
    validationErrors.username = '';
  };

  const deleteUser = async () => {
    deleting.value = true;

    try {
      await apiService.deleteUser(userId.value);

      notificationService.success(
        'User Deleted',
        `User "${user.value?.username}" has been deleted successfully.`
      );

      router.push('/users');
    } catch (err) {
      console.error('Error deleting user:', err);
      notificationService.error(
        'Delete Failed',
        err.message || 'Failed to delete user. Please try again.'
      );
      showDeleteModal.value = false;
    } finally {
      deleting.value = false;
    }
  };

  const goBack = () => {
    router.push('/users');
  };

  onMounted(() => {
    loadUser();
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

  /* Card Styling */
  .card {
    border-radius: 12px;
    overflow: hidden;
  }

  .card-header {
    background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  }

  /* Form Styling */
  .form-label {
    font-size: 0.875rem;
    margin-bottom: 0.5rem;
  }

  .form-control:focus {
    border-color: #0d6efd;
    box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.25);
  }

  .badge {
    font-weight: 500;
    padding: 0.5rem 0.75rem;
    font-size: 0.813rem;
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
      padding: 1rem !important;
    }

    .form-label {
      font-size: 0.813rem;
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
