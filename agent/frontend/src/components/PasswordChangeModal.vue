<template>
  <BaseModal
    :show="isVisible"
    title="Change Password"
    subtitle="Update your account password"
    icon="bi bi-key fs-3"
    size="sm"
    variant="primary"
    :loading="isChangingPassword"
    :save-button-props="{ text: 'Change Password', icon: 'bi-key' }"
    @close="handleClose"
    @save="changePassword"
  >
    <form class="modal-form" @submit.prevent="changePassword">
      <FormField
        id="currentPassword"
        v-model="passwordForm.currentPassword"
        type="password"
        label="Current Password"
        icon="bi-lock"
        placeholder="Enter current password"
        :error="validationErrors.currentPassword"
        required
      />

      <FormField
        id="newPassword"
        v-model="passwordForm.newPassword"
        type="password"
        label="New Password"
        icon="bi-key"
        placeholder="Enter new password"
        help-text="Password must be at least 8 characters long"
        help-icon="bi-info-circle"
        :error="validationErrors.newPassword"
        required
      />

      <FormField
        id="confirmPassword"
        v-model="passwordForm.confirmPassword"
        type="password"
        label="Confirm New Password"
        icon="bi-key-fill"
        placeholder="Confirm new password"
        :error="validationErrors.confirmPassword"
        required
      />

      <FormAlert
        v-if="passwordError"
        variant="danger"
        :message="passwordError"
      />

      <FormAlert
        v-if="passwordSuccess"
        variant="success"
        :message="passwordSuccess"
      />
    </form>
  </BaseModal>
</template>

<script setup>
  import { ref, watch } from 'vue';
  import { storeToRefs } from 'pinia';
  import { useUserStore } from '@/stores/userStore.js';
  import BaseModal from './BaseModal.vue';
  import { FormField, FormAlert } from '@/components/forms';

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
  .modal-form {
    padding: var(--spacing-sm) 0;
  }

  .modal-form :deep(.form-field) {
    margin-bottom: var(--spacing-lg);
  }

  .modal-form :deep(.form-field:last-of-type) {
    margin-bottom: var(--spacing-md);
  }

  .modal-form :deep(.form-alert) {
    margin-top: var(--spacing-md);
    margin-bottom: 0;
  }
</style>
