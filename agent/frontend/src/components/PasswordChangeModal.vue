<template>
  <BaseModal
    :show="isVisible"
    title="Change Password"
    subtitle="Update your account password"
    icon="bi bi-key fs-3"
    size="sm"
    variant="primary"
    :loading="isChangingPassword"
    :save-button-props="{ text: 'Change Password' }"
    @close="handleClose"
    @save="changePassword"
  >
    <form @submit.prevent="changePassword">
      <div class="mb-4">
        <label for="currentPassword" class="form-label fw-semibold">
          Current Password <span class="text-danger">*</span>
        </label>
        <input
          id="currentPassword"
          v-model="passwordForm.currentPassword"
          type="password"
          class="form-control"
          :class="{ 'is-invalid': validationErrors.currentPassword }"
          placeholder="Enter current password"
        />
        <div v-if="validationErrors.currentPassword" class="invalid-feedback">
          {{ validationErrors.currentPassword }}
        </div>
      </div>

      <div class="mb-4">
        <label for="newPassword" class="form-label fw-semibold">
          New Password <span class="text-danger">*</span>
        </label>
        <input
          id="newPassword"
          v-model="passwordForm.newPassword"
          type="password"
          class="form-control"
          :class="{ 'is-invalid': validationErrors.newPassword }"
          placeholder="Enter new password"
        />
        <div v-if="validationErrors.newPassword" class="invalid-feedback">
          {{ validationErrors.newPassword }}
        </div>
        <small v-else class="form-text text-muted">
          <i class="bi bi-info-circle me-1"></i>
          Password must be at least 8 characters long
        </small>
      </div>

      <div class="mb-4">
        <label for="confirmPassword" class="form-label fw-semibold">
          Confirm New Password <span class="text-danger">*</span>
        </label>
        <input
          id="confirmPassword"
          v-model="passwordForm.confirmPassword"
          type="password"
          class="form-control"
          :class="{ 'is-invalid': validationErrors.confirmPassword }"
          placeholder="Confirm new password"
        />
        <div v-if="validationErrors.confirmPassword" class="invalid-feedback">
          {{ validationErrors.confirmPassword }}
        </div>
      </div>

      <div v-if="passwordError" class="alert alert-danger py-2 mb-3" role="alert">
        <i class="bi bi-exclamation-circle me-2"></i>
        {{ passwordError }}
      </div>

      <div v-if="passwordSuccess" class="alert alert-success py-2 mb-3" role="alert">
        <i class="bi bi-check-circle me-2"></i>
        {{ passwordSuccess }}
      </div>
    </form>
  </BaseModal>
</template>

<script setup>
  import { ref, watch } from 'vue';
  import { storeToRefs } from 'pinia';
  import { useUserStore } from '@/stores/userStore.js';
  import BaseModal from './BaseModal.vue';

  const props = defineProps({
    isVisible: {
      type: Boolean,
      default: false,
    },
  });

  const emit = defineEmits(['close']);

  const userStore = useUserStore();
  const { isChangingPassword, passwordError, passwordSuccess, validationErrors } =
    storeToRefs(userStore);
  const { changePassword: storeChangePassword, resetPasswordState } = userStore;

  const passwordForm = ref({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  function handleClose() {
    resetPasswordForm();
    emit('close');
  }

  function resetPasswordForm() {
    passwordForm.value = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    };
    resetPasswordState();
  }

  async function changePassword() {
    const success = await storeChangePassword(
      passwordForm.value.currentPassword,
      passwordForm.value.newPassword,
      passwordForm.value.confirmPassword
    );
    if (success) {
      setTimeout(() => {
        handleClose();
      }, 1000);
    }
  }

  watch(
    () => props.isVisible,
    (newValue) => {
      if (newValue) {
        resetPasswordForm();
      }
    }
  );
</script>

<style scoped>
  .form-label {
    margin-bottom: 0.5rem;
    font-size: 1rem;
    color: #495057;
  }

  .form-control {
    font-size: 1rem;
    padding: 0.875rem 1rem;
    border: 1px solid #dee2e6;
    border-radius: 0.5rem;
    transition: all 0.2s ease;
  }

  .form-control:focus {
    border-color: #667eea;
    box-shadow: 0 0 0 0.2rem rgba(102, 126, 234, 0.25);
  }

  .form-text {
    display: block;
    margin-top: 0.5rem;
    font-size: 0.875rem;
  }

  .alert {
    border-radius: 0.5rem;
    border: none;
    display: flex;
    align-items: center;
  }

  .alert-danger {
    background-color: #fee;
    color: #c00;
  }

  .alert-success {
    background-color: #d4edda;
    color: #155724;
  }
</style>
