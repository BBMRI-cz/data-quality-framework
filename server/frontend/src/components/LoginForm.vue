<template>
  <div class="container-fluid vh-100 d-flex align-items-center justify-content-center bg-light">
    <div class="row w-100 justify-content-center">
      <div class="col-12 col-xl-10">
        <div class="card shadow-lg border-0 overflow-hidden mobile-card">
          <div class="row g-0 min-vh-75">
            <!-- Left side - Info panel (hidden on small screens) -->
            <div class="col-lg-7 d-none d-lg-flex">
              <div class="bg-dark text-white p-4 p-xl-5 d-flex flex-column justify-content-center w-100">
                <div>
                  <h1 class="display-5 fw-bold mb-3">Data Quality Metrics Server</h1>
                  <p class="lead mb-4">Central Server for Data Quality Monitoring and Reporting</p>

                  <div class="mb-4">
                    <div class="mb-4">
                      <h5 class="fw-semibold mb-2">Connected Repositories</h5>
                      <p class="text-light mb-0">Centralized monitoring of data quality across multiple connected data repositories and biobanks.</p>
                    </div>

                    <div class="mb-4">
                      <h5 class="fw-semibold mb-2">Automated Report Collection</h5>
                      <p class="text-light mb-0">Automatically collects and aggregates data quality reports from all connected repository endpoints.</p>
                    </div>

                    <div class="mb-4">
                      <h5 class="fw-semibold mb-2">Framework Oversight</h5>
                      <p class="text-light mb-0">Provides comprehensive oversight and governance for the entire data quality framework infrastructure.</p>
                    </div>
                  </div>

                  <div class="border-top border-secondary pt-4">
                    <div class="row text-center">
                      <div class="col-4">
                        <div class="h4 fw-bold mb-1">0</div>
                        <small class="text-uppercase text-light">Repositories</small>
                      </div>
                      <div class="col-4">
                        <div class="h4 fw-bold mb-1">0</div>
                        <small class="text-uppercase text-light">Generated Reports</small>
                      </div>
                      <div class="col-4">
                        <div class="h4 fw-bold mb-1">24/7</div>
                        <small class="text-uppercase text-light">Monitoring</small>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Right side - Login form -->
            <div class="col-lg-5 col-12">
              <div class="p-3 p-sm-4 p-lg-5 d-flex flex-column justify-content-center h-100">
                <div class="w-100 login-form-container">
                  <!-- Mobile header (visible only on small screens) -->
                  <div class="d-lg-none text-center mb-4 mobile-header">
                    <div class="brand-icon-mobile mx-auto mb-3">
                      <i class="bi bi-bar-chart-fill"></i>
                    </div>
                    <h2 class="h4 fw-bold text-dark mb-2">Data Quality Server</h2>
                    <p class="text-muted small">Central monitoring and reporting</p>
                  </div>

                  <div class="text-center mb-4">
                    <h1 class="h4 h-lg-3 fw-bold text-dark mb-2">Welcome Back</h1>
                    <p class="text-muted mb-0 small">Please sign in to your account</p>
                  </div>

                  <form @submit.prevent="handleLogin" class="login-form">
                    <div class="mb-3">
                      <label for="username" class="form-label fw-semibold">Username</label>
                      <input
                        id="username"
                        v-model="form.username"
                        type="text"
                        class="form-control form-control-mobile"
                        :class="{ 'is-invalid': errors.username }"
                        placeholder="Enter your username"
                        :disabled="authStore.isLoading"
                        required
                        autocomplete="username"
                      />
                      <div v-if="errors.username" class="invalid-feedback">
                        {{ errors.username }}
                      </div>
                    </div>

                    <div class="mb-3">
                      <label for="password" class="form-label fw-semibold">Password</label>
                      <input
                        id="password"
                        v-model="form.password"
                        type="password"
                        class="form-control form-control-mobile"
                        :class="{ 'is-invalid': errors.password }"
                        placeholder="Enter your password"
                        :disabled="authStore.isLoading"
                        required
                        autocomplete="current-password"
                      />
                      <div v-if="errors.password" class="invalid-feedback">
                        {{ errors.password }}
                      </div>
                    </div>

                    <div class="mb-4">
                      <div class="form-check">
                        <input
                          v-model="form.rememberMe"
                          type="checkbox"
                          class="form-check-input"
                          id="rememberMe"
                          :disabled="authStore.isLoading"
                        />
                        <label class="form-check-label small" for="rememberMe">
                          Remember me
                        </label>
                      </div>
                    </div>

                    <button
                      type="submit"
                      class="btn btn-primary w-100 py-3 fw-semibold btn-mobile"
                      :disabled="authStore.isLoading || !isFormValid"
                    >
                      <span v-if="authStore.isLoading" class="spinner-border spinner-border-sm me-2" role="status"></span>
                      {{ authStore.isLoading ? 'Signing in...' : 'Sign In' }}
                    </button>

                    <div v-if="authStore.error" class="alert alert-danger mt-3 mb-0">
                      {{ authStore.error }}
                    </div>
                  </form>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { authStore } from '../stores/authStore.js'
import { apiService } from '../services/apiService.js'
import { notificationService } from '../services/notificationService.js'

const router = useRouter()

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
    const response = await apiService.login(form.username, form.password)
    authStore.setUser(response.user || { username: form.username }, response.token)

    notificationService.success(
      'Login Successful',
      `Welcome back, ${response.user?.username || form.username}!`
    )

    form.password = ''
    if (!form.rememberMe) {
      form.username = ''
    }

    // Redirect to the intended route or dashboard
    const redirectPath = authStore.getRedirectPath()
    await router.push(redirectPath)

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
.min-vh-75 {
  min-height: 75vh;
}

@media (max-width: 991.98px) {
  .min-vh-75 {
    min-height: auto;
  }
}

.card {
  border-radius: 12px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
}

.btn-primary:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
  transform: translateY(-1px);
}

.btn-primary:active {
  transform: translateY(0);
}

.form-control:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 0.2rem rgba(102, 126, 234, 0.25);
}

/* Mobile-specific styles */
@media (max-width: 768px) {
  .mobile-card {
    margin: 1rem;
    border-radius: 1rem !important;
  }

  .login-form-container {
    max-width: 100% !important;
  }

  .mobile-header {
    padding: 1rem 0;
  }

  .brand-icon-mobile {
    width: 60px;
    height: 60px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 15px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 24px;
  }

  .form-control-mobile {
    font-size: 16px; /* Prevent zoom on iOS */
    padding: 0.75rem 1rem;
    min-height: 48px;
  }

  .btn-mobile {
    min-height: 48px;
    font-size: 1rem;
  }

  .form-check-input {
    width: 1.2em;
    height: 1.2em;
  }

  .form-check-label {
    padding-left: 0.5rem;
  }
}

@media (max-width: 576px) {
  .container-fluid {
    padding: 0.5rem;
  }

  .card {
    margin: 0.5rem;
  }

  .p-3 {
    padding: 1.5rem !important;
  }
}

/* Improve touch targets */
@media (hover: none) and (pointer: coarse) {
  .btn {
    min-height: 44px;
  }

  .form-control {
    min-height: 44px;
  }

  .form-check-input {
    width: 1.25em;
    height: 1.25em;
  }
}
</style>
