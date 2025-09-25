<template>
  <div class="modal fade" id="passwordChangeModal" tabindex="-1" aria-labelledby="passwordChangeModalLabel"
       aria-hidden="true" ref="modalElement">
    <div class="modal-dialog modal-dialog-centered" role="document">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" id="passwordChangeModalLabel">Change Password</h5>
          <button type="button" class="btn-close" @click="_close" aria-label="Close"></button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="changePassword">
            <div class="mb-3">
              <label for="currentPassword" class="form-label">Current Password</label>
              <input
                  type="password"
                  class="form-control"
                  id="currentPassword"
                  v-model="passwordForm.currentPassword"
                  required
              >
            </div>
            <div class="mb-3">
              <label for="newPassword" class="form-label">New Password</label>
              <input
                  type="password"
                  class="form-control"
                  id="newPassword"
                  v-model="passwordForm.newPassword"
                  minlength="8"
                  required
              >
            </div>
            <div class="mb-3">
              <label for="confirmPassword" class="form-label">Confirm New Password</label>
              <input
                  type="password"
                  class="form-control"
                  id="confirmPassword"
                  v-model="passwordForm.confirmPassword"
                  minlength="8"
                  required
              >
            </div>
            <div v-if="passwordError" class="alert alert-danger py-2" role="alert">
              {{ passwordError }}
            </div>
            <div v-if="passwordSuccess" class="alert alert-success py-2" role="alert">
              {{ passwordSuccess }}
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="_close">Cancel</button>
          <button type="button" class="btn btn-primary" @click="changePassword" :disabled="isChangingPassword">
            <span v-if="isChangingPassword" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
            Change Password
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, defineExpose, watch } from 'vue';
import { Modal } from 'bootstrap';
import { getUsername } from '../js/api.js';
import { useUserStore } from '../stores/userStore.js';
const props = defineProps({
  isVisible: {
    type: Boolean,
    default: false
  }
});
const emit = defineEmits(['close']);
let modalElement = ref(null);
let modalInstance = null;
const username = getUsername();
const { isChangingPassword, passwordError, passwordSuccess, changePassword: storeChangePassword } = useUserStore();
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});
function _open() {
  if (modalInstance) {
    resetPasswordForm();
    modalInstance.show();
  }
}
function _close() {
  if (modalInstance) {
    modalInstance.hide();
    resetPasswordForm();
    emit('close');
  }
}
function resetPasswordForm() {
  passwordForm.value = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  };
  passwordError.value = '';
  passwordSuccess.value = '';
}
async function changePassword() {
  const success = await storeChangePassword(
    passwordForm.value.currentPassword,
    passwordForm.value.newPassword,
    passwordForm.value.confirmPassword
  );
  if (success) {
    setTimeout(() => {
      _close();
    }, 2000);
  }
}
watch(() => props.isVisible, (newValue) => {
  if (newValue) {
    _open();
  } else {
    _close();
  }
});
onMounted(() => {
  modalInstance = new Modal(modalElement.value, {
    backdrop: true,
    keyboard: true,
    focus: true,
  });

modalElement.value.addEventListener('hidden.bs.modal', () => {
  resetPasswordForm();
  emit('close');
});

modalElement.value.addEventListener('shown.bs.modal', () => {
  const firstInput = modalElement.value.querySelector('#currentPassword');
  if (firstInput) {
    firstInput.focus();
  }
});
});
defineExpose({ open: _open, close: _close });
</script>

<style scoped>
</style>