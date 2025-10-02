<template>
  <div class="login-container">
    <div class="login-wrapper">
      <!-- Left side - Informative content -->
      <div class="info-panel">
        <div class="info-content">
          <h1>Data Quality Metrics Server</h1>
          <p class="subtitle">Central Server for Data Quality Monitoring and Reporting</p>

          <div class="features">
            <div class="feature-item">
              <div class="feature-text">
                <h3>Connected Repositories</h3>
                <p>Centralized monitoring of data quality across multiple connected data repositories and biobanks.</p>
              </div>
            </div>

            <div class="feature-item">
              <div class="feature-text">
                <h3>Automated Report Collection</h3>
                <p>Automatically collects and aggregates data quality reports from all connected repository endpoints.</p>
              </div>
            </div>

            <div class="feature-item">
              <div class="feature-text">
                <h3>Framework Oversight</h3>
                <p>Provides comprehensive oversight and governance for the entire data quality framework infrastructure.</p>
              </div>
            </div>
          </div>

          <div class="stats">
            <div class="stat">
              <span class="stat-number">0</span>
              <span class="stat-label">Repositories</span>
            </div>
            <div class="stat">
              <span class="stat-number">0</span>
              <span class="stat-label">Generated Reports</span>
            </div>
            <div class="stat">
              <span class="stat-number">24/7</span>
              <span class="stat-label">Monitoring</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right side - Login form -->
      <div class="login-panel">
        <div class="login-card">
          <div class="login-header">
            <h1 class="login-title">Welcome Back</h1>
            <p class="login-subtitle">Please sign in to your account</p>
          </div>

          <form @submit.prevent="handleLogin" class="login-form-content">
            <div class="form-group">
              <label for="username" class="form-label">Username</label>
              <input
                id="username"
                v-model="form.username"
                type="text"
                class="form-input"
                :class="{ 'form-input--error': errors.username }"
                placeholder="Enter your username"
                :disabled="authStore.isLoading"
                required
              />
              <span v-if="errors.username" class="form-error">{{ errors.username }}</span>
            </div>

            <div class="form-group">
              <label for="password" class="form-label">Password</label>
              <input
                id="password"
                v-model="form.password"
                type="password"
                class="form-input"
                :class="{ 'form-input--error': errors.password }"
                placeholder="Enter your password"
                :disabled="authStore.isLoading"
                required
              />
              <span v-if="errors.password" class="form-error">{{ errors.password }}</span>
            </div>

            <div class="form-group">
              <label class="checkbox-label">
                <input
                  v-model="form.rememberMe"
                  type="checkbox"
                  class="checkbox-input"
                  :disabled="authStore.isLoading"
                />
                <span class="checkbox-custom"></span>
                Remember me
              </label>
            </div>

            <button
              type="submit"
              class="login-button"
              :disabled="authStore.isLoading || !isFormValid"
              :class="{ 'login-button--loading': authStore.isLoading }"
            >
              <span v-if="authStore.isLoading" class="loading-spinner"></span>
              {{ authStore.isLoading ? 'Signing in...' : 'Sign In' }}
            </button>

            <div v-if="authStore.error" class="login-error">
              {{ authStore.error }}
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, onMounted, watch } from 'vue'
import { authStore } from '../stores/authStore.js'
import { apiService } from '../services/apiService.js'
import { notificationService } from '../services/notificationService.js'

const form = reactive({
  username: '',
  password: '',
  rememberMe: false
})

const errors = reactive({
  username: '',
  password: ''
})

const isFormValid = computed(() => {
  return form.username.trim() && form.password.trim() && !authStore.isLoading
})

const validateForm = () => {
  // Clear previous errors
  errors.username = ''
  errors.password = ''

  let isValid = true

  if (!form.username.trim()) {
    errors.username = 'Username is required'
    isValid = false
  }

  if (!form.password.trim()) {
    errors.password = 'Password is required'
    isValid = false
  } else if (form.password.length < 3) {
    errors.password = 'Password must be at least 3 characters'
    isValid = false
  }

  return isValid
}

const handleLogin = async () => {
  if (!validateForm()) {
    return
  }

  authStore.setLoading(true)
  authStore.clearError()

  try {
    const credentials = btoa(`${form.username}:${form.password}`)
    const user = await apiService.login(form.username, form.password)

    authStore.setUser(user, credentials)

    notificationService.success(
      'Login Successful',
      `Welcome back, ${user.username || form.username}!`
    )

    form.password = ''
    if (!form.rememberMe) {
      form.username = ''
    }

  } catch (error) {
    console.error('Login failed:', error)
    authStore.setError('Login failed. Please check your credentials and try again.')

    notificationService.error(
      'Login Failed',
      'Invalid username or password. Please try again.'
    )
  } finally {
    authStore.setLoading(false)
  }
}

// Load remembered username on component mount
onMounted(() => {
  const rememberedUsername = localStorage.getItem('rememberedUsername')
  if (rememberedUsername) {
    form.username = rememberedUsername
    form.rememberMe = true
  }
})

// Save/remove remembered username when checkbox changes
const saveRememberedUsername = () => {
  if (form.rememberMe && form.username.trim()) {
    localStorage.setItem('rememberedUsername', form.username)
  } else {
    localStorage.removeItem('rememberedUsername')
  }
}

// Watch for remember me changes
watch(() => form.rememberMe, saveRememberedUsername)
watch(() => form.username, () => {
  if (form.rememberMe) {
    saveRememberedUsername()
  }
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  padding: 2rem;
}

.login-wrapper {
  display: flex;
  max-width: 1200px;
  width: 100%;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  min-height: 600px;
}

/* Left panel - Info content */
.info-panel {
  flex: 1;
  background: #374151;
  color: white;
  padding: 3rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.info-content h1 {
  font-size: 2.5rem;
  font-weight: 700;
  margin: 0 0 0.5rem 0;
  color: white;
}

.subtitle {
  font-size: 1.2rem;
  color: #d1d5db;
  margin: 0 0 2.5rem 0;
  line-height: 1.5;
}

.features {
  margin-bottom: 2.5rem;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 2rem;
  gap: 1rem;
}

.feature-icon {
  font-size: 2rem;
  flex-shrink: 0;
}

.feature-text h3 {
  font-size: 1.1rem;
  font-weight: 600;
  margin: 0 0 0.5rem 0;
  color: white;
}

.feature-text p {
  font-size: 0.9rem;
  color: #d1d5db;
  margin: 0;
  line-height: 1.5;
}

.stats {
  display: flex;
  gap: 2rem;
  padding-top: 2rem;
  border-top: 1px solid #4b5563;
}

.stat {
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
}

.stat-label {
  font-size: 0.8rem;
  color: #d1d5db;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Right panel - Login form */
.login-panel {
  flex: 0 0 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background: white;
}

.login-card {
  width: 100%;
  max-width: 350px;
}

.login-header {
  text-align: center;
  margin-bottom: 2rem;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.login-subtitle {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
}

.login-form-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.form-input {
  padding: 12px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  background: white;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input--error {
  border-color: #ef4444;
}

.form-input--error:focus {
  border-color: #ef4444;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

.form-input:disabled {
  background-color: #f9fafb;
  color: #9ca3af;
  cursor: not-allowed;
}

.form-error {
  font-size: 14px;
  color: #ef4444;
  margin-top: 4px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  user-select: none;
}

.checkbox-input {
  position: absolute;
  opacity: 0;
  cursor: pointer;
}

.checkbox-custom {
  width: 18px;
  height: 18px;
  border: 2px solid #d1d5db;
  border-radius: 4px;
  position: relative;
  transition: all 0.2s ease;
}

.checkbox-input:checked + .checkbox-custom {
  background-color: #3b82f6;
  border-color: #3b82f6;
}

.checkbox-input:checked + .checkbox-custom::after {
  content: '✓';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 12px;
  font-weight: bold;
}

.checkbox-input:disabled + .checkbox-custom {
  background-color: #f3f4f6;
  border-color: #d1d5db;
  cursor: not-allowed;
}

.login-button {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 14px 20px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.1s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;
}

.login-button:hover:not(:disabled) {
  background: #2563eb;
  transform: translateY(-1px);
}

.login-button:active:not(:disabled) {
  transform: translateY(0);
}

.login-button:disabled {
  background: #9ca3af;
  cursor: not-allowed;
  transform: none;
}

.login-button--loading {
  pointer-events: none;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top: 2px solid currentColor;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.login-error {
  padding: 12px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 14px;
  text-align: center;
}

@media (max-width: 480px) {
  .login-card {
    padding: 24px;
    margin: 16px;
  }

  .login-title {
    font-size: 24px;
  }
}
</style>
