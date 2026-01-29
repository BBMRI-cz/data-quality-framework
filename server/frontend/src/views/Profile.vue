<template>
  <div class="container-fluid py-3 py-md-4">
    <PageHeader
      title="Profile"
      subtitle="Manage your account settings and password"
      icon="bi bi-person-circle"
    />

    <div class="row justify-content-center">
      <div class="col-12 col-lg-8">

        <!-- User Information Card -->
        <div class="card border-0 shadow-sm mb-4">
          <div class="card-body p-4">
            <h3 class="h5 fw-bold mb-3">User Information</h3>
            <div class="row g-3">
              <div class="col-md-6">
                <label class="form-label text-muted small">Username</label>
                <p class="fw-semibold mb-0">{{ user?.username || 'N/A' }}</p>
              </div>
              <div class="col-md-6">
                <label class="form-label text-muted small">User ID</label>
                <p class="fw-semibold mb-0">{{ user?.id || 'N/A' }}</p>
              </div>
              <div class="col-md-6" v-if="user?.agentId">
                <label class="form-label text-muted small">Agent ID</label>
                <p class="fw-semibold mb-0">{{ user.agentId }}</p>
              </div>
              <div class="col-md-6" v-if="user?.roles && user.roles.length > 0">
                <label class="form-label text-muted small">Roles</label>
                <p class="fw-semibold mb-0">
                  <span v-for="role in user.roles" :key="role" class="badge bg-primary me-1">
                    {{ role }}
                  </span>
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Change Password Card -->
        <div class="card border-0 shadow-sm">
          <div class="card-body p-4">
            <h3 class="h5 fw-bold mb-3">Change Password</h3>

            <form @submit.prevent="handlePasswordChange">
              <div class="mb-3">
                <label for="currentPassword" class="form-label">Current Password</label>
                <input
                  type="password"
                  class="form-control"
                  :class="{ 'is-invalid': errors.currentPassword }"
                  id="currentPassword"
                  v-model="passwordForm.currentPassword"
                  required
                  autocomplete="current-password"
                >
                <div class="invalid-feedback" v-if="errors.currentPassword">
                  {{ errors.currentPassword }}
                </div>
              </div>

              <div class="mb-3">
                <label for="newPassword" class="form-label">New Password</label>
                <input
                  type="password"
                  class="form-control"
                  :class="{ 'is-invalid': errors.newPassword }"
                  id="newPassword"
                  v-model="passwordForm.newPassword"
                  required
                  autocomplete="new-password"
                >
                <div class="invalid-feedback" v-if="errors.newPassword">
                  {{ errors.newPassword }}
                </div>
                <div class="form-text">
                  Password must be at least 8 characters long and contain only letters, digits or special characters.
                </div>
              </div>

              <div class="mb-3">
                <label for="confirmPassword" class="form-label">Confirm New Password</label>
                <input
                  type="password"
                  class="form-control"
                  :class="{ 'is-invalid': errors.confirmPassword }"
                  id="confirmPassword"
                  v-model="passwordForm.confirmPassword"
                  required
                  autocomplete="new-password"
                >
                <div class="invalid-feedback" v-if="errors.confirmPassword">
                  {{ errors.confirmPassword }}
                </div>
              </div>

              <div class="d-flex gap-2">
                <button
                  type="submit"
                  class="btn btn-primary"
                  :disabled="isSubmitting"
                >
                  <span v-if="isSubmitting" class="spinner-border spinner-border-sm me-2"></span>
                  {{ isSubmitting ? 'Changing...' : 'Change Password' }}
                </button>
                <button
                  type="button"
                  class="btn btn-outline-secondary"
                  @click="resetForm"
                  :disabled="isSubmitting"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, ref } from 'vue'
import { authStore } from '../stores/authStore.js'
import { apiService } from '../services/apiService.js'
import { notificationService } from '../services/notificationService.js'
import PageHeader from '../components/PageHeader.vue'

const user = computed(() => authStore.user)

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const errors = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const isSubmitting = ref(false)

const validatePasswordForm = () => {
  // Clear previous errors
  errors.currentPassword = ''
  errors.newPassword = ''
  errors.confirmPassword = ''

  let isValid = true

  // Validate current password
  if (!passwordForm.currentPassword.trim()) {
    errors.currentPassword = 'Current password is required'
    isValid = false
  }

  // Validate new password
  if (!passwordForm.newPassword.trim()) {
    errors.newPassword = 'New password is required'
    isValid = false
  } else if (passwordForm.newPassword.length < 8) {
    errors.newPassword = 'Password must be at least 8 characters long'
    isValid = false
  } else if (!/^[a-zA-Z0-9!@#$%^&*(),.?":{}|<>_-]{8,}$/.test(passwordForm.newPassword)) {
    errors.newPassword = 'Password contains invalid characters'
    isValid = false
  }

  // Validate confirm password
  if (!passwordForm.confirmPassword.trim()) {
    errors.confirmPassword = 'Please confirm your new password'
    isValid = false
  } else if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    errors.confirmPassword = 'Passwords do not match'
    isValid = false
  }

  return isValid
}

const handlePasswordChange = async () => {
  if (!validatePasswordForm()) {
    return
  }

  if (!user.value?.id) {
    notificationService.error('Error', 'User ID not found')
    return
  }

  isSubmitting.value = true

  try {
    await apiService.changePassword(
      user.value.id,
      passwordForm.currentPassword,
      passwordForm.newPassword,
      passwordForm.confirmPassword
    )

    notificationService.success(
      'Password Changed',
      'Your password has been successfully updated'
    )

    resetForm()
  } catch (error) {
    console.error('Password change failed:', error)
    notificationService.error(
      'Password Change Failed',
      error.message || 'Unable to change password. Please check your current password and try again.'
    )
  } finally {
    isSubmitting.value = false
  }
}

const resetForm = () => {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  errors.currentPassword = ''
  errors.newPassword = ''
  errors.confirmPassword = ''
}
</script>

<style scoped>
.badge {
  font-weight: 500;
  font-size: 0.85rem;
}

.form-label {
  font-weight: 500;
  margin-bottom: 0.5rem;
}

.card {
  border-radius: 12px;
}

@media (max-width: 768px) {
  .card-body {
    padding: 1.5rem !important;
  }
}
</style>

