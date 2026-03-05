<template>
  <div class="card shadow-lg border-0 overflow-hidden mobile-card">
    <div class="row g-0">
      <!-- Left side - Info panel (hidden on small screens) -->
      <div class="col-lg-6 d-none d-lg-flex">
        <div class="info-panel p-3 p-lg-4 d-flex flex-column justify-content-center w-100">
          <div>
            <h1 class="display-5 fw-bold mb-3">Data Quality Server</h1>
            <p class="lead mb-4">Central Server for Data Quality Monitoring and Reporting</p>

            <div class="mb-4">
              <div class="mb-4">
                <h5 class="fw-semibold mb-2">Connected Repositories</h5>
                <p class="text-light mb-0">
                  Centralized monitoring of Data Quality across multiple connected Data
                  Repositories.
                </p>
              </div>

              <div class="mb-4">
                <h5 class="fw-semibold mb-2">Automated Report Aggregation</h5>
                <p class="text-light mb-0">
                  Periodically receives and aggregates Data Quality Reports from all connected Data
                  Repositories.
                </p>
              </div>

              <div class="mb-4">
                <h5 class="fw-semibold mb-2">Central Interface</h5>
                <p class="text-light mb-0">
                  Provides comprehensive oversight and governance of the Federated Data Quality
                  System.
                </p>
              </div>
            </div>

            <div class="border-top border-secondary pt-4">
              <div class="row text-center">
                <div class="col-6">
                  <div class="h4 fw-bold mb-1 count-animation">{{ displayAgentCount }}</div>
                  <small class="text-uppercase text-light">Repositories</small>
                </div>
                <div class="col-6">
                  <div class="h4 fw-bold mb-1 count-animation">{{ displayReportCount }}</div>
                  <small class="text-uppercase text-light">Generated Reports</small>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Right side - Login form -->
      <div class="col-lg-6 col-12">
        <div class="px-3 py-5 d-flex flex-column justify-content-center login-form-wrapper">
          <div class="w-100 login-form-container">
            <!-- Mobile header (visible only on small screens) -->
            <div class="d-lg-none text-center mb-4 mobile-header">
              <div class="brand-icon-mobile mx-auto mb-3">
                <i class="bi bi-bar-chart-fill"></i>
              </div>
              <h2 class="h4 fw-bold text-dark mb-2">Data Quality Server</h2>
              <p class="text-muted small">Central monitoring and reporting</p>
            </div>

            <div class="text-center mb-5">
              <div class="logo-container mb-4">
                <img src="/logo.svg" alt="Logo" class="login-logo" />
              </div>
              <h1 class="h5 fw-bold text-dark mb-2">Welcome</h1>
              <p class="text-muted mb-0 small">Please sign in to your account</p>
            </div>

            <form class="login-form mx-auto" @submit.prevent="handleLogin">
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

              <button
                type="submit"
                class="btn btn-primary w-100 py-2 fw-semibold btn-mobile"
                :disabled="authStore.isLoading || !isFormValid"
              >
                <span
                  v-if="authStore.isLoading"
                  class="spinner-border spinner-border-sm me-2"
                  role="status"
                ></span>
                {{ authStore.isLoading ? 'Signing in...' : 'Sign In' }}
              </button>
              <template v-if="isOidcConfigured">
                <div class="d-flex align-items-center my-3">
                  <hr class="flex-grow-1" />
                  <span class="mx-2 text-muted small">OR</span>
                  <hr class="flex-grow-1" />
                </div>

                <button
                  type="button"
                  class="btn btn-primary w-100 py-2 fw-semibold btn-mobile d-flex align-items-center justify-content-center gap-2 oidc-button"
                  :disabled="authStore.isLoading || isOidcLoading"
                  @click="handleOidcLogin"
                >
                  <span v-if="!isOidcLoading" class="oidc-provider-badge">
                    <img
                      v-if="oidcLogo"
                      :src="oidcLogo"
                      :alt="`${oidcDisplayName} logo`"
                      class="oidc-provider-logo"
                    />
                    <svg
                      v-else
                      class="oidc-icon"
                      xmlns="http://www.w3.org/2000/svg"
                      viewBox="0 0 640 640"
                    >
                      <path
                        d="M367.5 496L299.5 528C184.5 517.7 96 456.5 96 382.2C96 310.7 178.5 251.2 287.7 237.9L287.7 280.9C216.2 293.4 163.7 333.9 163.7 382.2C163.7 433.2 222.2 475.5 299.4 485.2L299.4 145.2L367.4 112L367.4 496L367.5 496zM544 355L412.7 326.5L449.5 305.8C430 294.3 406 285.8 379.5 281L379.5 238C425.7 243.5 467.2 257.5 499.8 277.3L534.8 257.5L544 355z"
                      />
                    </svg>
                  </span>
                  <span
                    v-if="isOidcLoading"
                    class="spinner-border spinner-border-sm me-2"
                    role="status"
                  ></span>
                  <span v-if="isOidcLoading" class="visually-hidden"
                    >Redirecting to login provider...</span
                  >
                  <span class="oidc-provider-label" :title="`${oidcDisplayName}`">
                    {{ isOidcLoading ? 'Redirecting...' : oidcDisplayName }}
                  </span>
                </button>
              </template>

              <div v-if="authStore.error" class="alert alert-danger mt-3 mb-0">
                {{ authStore.error }}
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { reactive, computed, ref, onMounted } from 'vue';
  import { useRouter, useRoute } from 'vue-router';
  import { authStore } from '../stores/authStore.js';
  import { apiService } from '../services/apiService.js';
  import { notificationService } from '../services/notificationService.js';
  import { initializeOidc, signinWithOidc } from '../utils/oidc.js';
  import settingsStore from '../stores/settingsStore.js';

  const router = useRouter();
  const route = useRoute();
  const agentCount = ref(0);
  const reportCount = ref(0);
  const displayAgentCount = ref(0);
  const displayReportCount = ref(0);
  const isOidcLoading = ref(false);
  const sessionExpiredHandled = ref(false);

  const { settings } = settingsStore;

  const isOidcConfigured = computed(() => {
    return settings.value?.oidcAuthority && settings.value.oidcAuthority.trim() !== '';
  });

  const oidcDisplayName = computed(() => settings.value?.oidcAuthorityName?.trim() || 'OIDC');
  const oidcLogo = computed(() => settings.value?.oidcAuthorityLogo?.trim() || null);

  const form = reactive({
    username: '',
    password: '',
  });

  const errors = reactive({
    username: '',
    password: '',
  });

  // Animate count from current value to target value
  const animateCount = (displayRef, targetValue, duration = 1500) => {
    const startValue = displayRef.value;
    const startTime = Date.now();
    const difference = targetValue - startValue;

    const updateCount = () => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);

      // Easing function for smooth animation (ease-out)
      const easeOut = 1 - Math.pow(1 - progress, 3);

      displayRef.value = Math.round(startValue + difference * easeOut);

      if (progress < 1) {
        requestAnimationFrame(updateCount);
      }
    };

    requestAnimationFrame(updateCount);
  };

  onMounted(async () => {
    if (route.query.sessionExpired === 'true') {
      sessionExpiredHandled.value = true;
      router.replace({ name: 'Login', query: {} });
    }

    try {
      await settingsStore.fetchOidcSettings();

      if (settingsStore.settings.value?.oidcAuthority && sessionExpiredHandled.value) {
        try {
          await initializeOidc(true);
        } catch (error) {
          console.error('Failed to initialize OIDC on mount:', error);
        }
      }
    } catch (error) {
      console.error('Failed to fetch OIDC settings:', error);
    }

    try {
      const counts = await apiService.getCounts();
      agentCount.value = counts.agents || 0;
      reportCount.value = counts.reports || 0;

      // Animate the counts
      animateCount(displayAgentCount, agentCount.value);
      animateCount(displayReportCount, reportCount.value);
    } catch (error) {
      console.error('Failed to fetch counts:', error);
      agentCount.value = 0;
      reportCount.value = 0;
    }
  });

  const isFormValid = computed(() => {
    return form.username.trim() && form.password.trim() && !authStore.isLoading;
  });

  const validateForm = () => {
    // Clear previous errors
    errors.username = '';
    errors.password = '';

    let isValid = true;

    if (!form.username.trim()) {
      errors.username = 'Username is required';
      isValid = false;
    }

    if (!form.password.trim()) {
      errors.password = 'Password is required';
      isValid = false;
    } else if (form.password.length < 3) {
      errors.password = 'Password must be at least 3 characters';
      isValid = false;
    }

    return isValid;
  };

  const handleOidcLogin = async () => {
    isOidcLoading.value = true;
    authStore.clearError();

    try {
      if (!settingsStore.settings.value?.oidcAuthority) {
        throw new Error('OIDC settings not configured. Please contact your administrator.');
      }

      await initializeOidc(false);

      if (sessionExpiredHandled.value) {
        sessionExpiredHandled.value = false;
      }

      await signinWithOidc();

      isOidcLoading.value = false;
      authStore.setError('OIDC login failed to redirect. Please try again.');
    } catch (error) {
      isOidcLoading.value = false;
      authStore.setError(error.message || 'OIDC login failed. Please try again.');
    }
  };

  const handleLogin = async () => {
    if (!validateForm()) {
      return;
    }

    authStore.setLoading(true);
    authStore.clearError();

    try {
      const response = await apiService.login(form.username, form.password);
      await authStore.setUser(response.user || { username: form.username }, response.token);

      notificationService.success(
        'Login Successful',
        `Welcome back, ${response.user?.username || form.username}!`
      );

      form.password = '';
      form.username = '';

      // Redirect to the intended route or dashboard
      const redirectPath = authStore.getRedirectPath();
      await router.push(redirectPath);
    } catch (error) {
      console.error('Login failed:', error);
      authStore.setError('Login failed. Please check your credentials and try again.');
    } finally {
      authStore.setLoading(false);
    }
  };
</script>

<style scoped>
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

  .info-panel {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    position: relative;
    overflow: hidden;
  }

  .info-panel::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(255, 255, 255, 0.05);
    backdrop-filter: blur(10px);
    pointer-events: none;
  }

  .info-panel > div {
    position: relative;
    z-index: 1;
  }

  .logo-container {
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .login-logo {
    width: 200px;
    height: 200px;
    object-fit: contain;
  }

  .login-form {
    max-width: 280px;
  }

  .count-animation {
    transition: transform 0.3s ease;
    display: block;
  }

  .count-animation:hover {
    transform: scale(1.1);
  }

  .login-form-wrapper {
    min-height: auto;
  }

  /* Mobile-specific styles */
  @media (max-width: 768px) {
    .login-logo {
      width: 120px;
      height: 120px;
    }

    .login-form {
      max-width: 100%;
    }

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
  }

  @media (max-width: 576px) {
    .card {
      margin: 0.5rem;
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
  }

  .oidc-provider-badge {
    background-color: rgba(255, 255, 255, 0.1);
    border-radius: 999px;
    width: 44px;
    height: 44px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0.2rem;
  }

  .oidc-provider-logo {
    width: 100%;
    height: 100%;
    max-width: 32px;
    max-height: 32px;
    object-fit: contain;
  }

  .oidc-icon {
    width: 28px;
    height: 28px;
    fill: currentColor;
  }

  .oidc-button {
    gap: 0.5rem;
    padding: 0.45rem 1rem;
    max-height: 56px;
    font-size: 0.95rem;
    justify-content: center;
    text-align: center;
  }

  .oidc-provider-label {
    flex: 0 1 auto;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font: inherit;
    font-weight: inherit;
    color: inherit;
  }
</style>
