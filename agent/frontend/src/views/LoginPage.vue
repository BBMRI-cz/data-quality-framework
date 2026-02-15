<template>
  <div class="login-page">
    <div class="login-page__container">
      <div class="login-card">
        <div class="login-card__content">
          <!-- Left side - Info panel (hidden on small screens) -->
          <div class="login-card__info">
            <LoginInfoPanel />
          </div>

          <!-- Right side - Login form -->
          <div class="login-card__form-wrapper">
            <div class="login-form-container">
              <!-- Mobile header (visible only on small screens) -->
              <header class="mobile-header">
                <div class="mobile-header__icon" aria-hidden="true">
                  <i class="bi bi-bar-chart-fill"></i>
                </div>
                <h2 class="mobile-header__title">Data Quality Agent</h2>
                <p class="mobile-header__subtitle">Local repository monitoring</p>
              </header>

              <div class="login-form-header">
                <img src="/logo.svg" alt="Data Quality Agent Logo" class="login-logo" />
                <h1 class="login-form-header__title">Welcome</h1>
                <p class="login-form-header__subtitle">Please sign in to your account</p>
              </div>

              <form novalidate @submit.prevent="login">
                <div class="form-group">
                  <label for="username" class="form-label">Username</label>
                  <input
                    id="username"
                    v-model="username"
                    type="text"
                    class="form-control-base"
                    placeholder="Enter your username"
                    :disabled="loading"
                    :aria-describedby="error ? 'login-error' : undefined"
                    required
                    autocomplete="username"
                  />
                </div>

                <div class="form-group">
                  <label for="password" class="form-label">Password</label>
                  <div class="password-input-wrapper">
                    <input
                      id="password"
                      v-model="password"
                      :type="showPassword ? 'text' : 'password'"
                      class="form-control-base"
                      placeholder="Enter your password"
                      :disabled="loading"
                      :aria-describedby="error ? 'login-error' : undefined"
                      required
                      autocomplete="current-password"
                    />
                    <button
                      type="button"
                      class="password-toggle-btn"
                      :disabled="loading"
                      :aria-label="showPassword ? 'Hide password' : 'Show password'"
                      tabindex="-1"
                      @click="togglePasswordVisibility"
                    >
                      <i :class="showPassword ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                    </button>
                  </div>
                </div>

                <button type="submit" class="btn-submit-full" :disabled="loading">
                  <span v-if="loading" class="spinner-border spinner-border-sm" role="status">
                    <span class="visually-hidden">Loading...</span>
                  </span>
                  {{ loading ? 'Signing in...' : 'Sign In' }}
                </button>

                <div v-if="error" id="login-error" class="alert-base alert-danger" role="alert">
                  {{ error }}
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
      <Copyright />
    </div>
  </div>
</template>

<script setup>
  import { onMounted } from 'vue';
  import { useRoute } from 'vue-router';
  import Copyright from '@/components/Copyright.vue';
  import LoginInfoPanel from '@/components/LoginInfoPanel.vue';
  import { useLoginForm } from '@/composables/useLoginForm.js';
  import { notificationService } from '@/services/notificationService.js';

  const route = useRoute();

  const { username, password, loading, error, showPassword, togglePasswordVisibility, login } =
    useLoginForm();

  onMounted(() => {
    if (route.query.sessionExpired === 'true') {
      notificationService.warning(
        'Session Expired',
        'Your session has expired. Please log in again.'
      );
    }
  });
</script>

<style scoped>
  .login-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: var(--color-gray-100);
    padding: var(--spacing-md);
  }

  .login-page__container {
    width: 100%;
    max-width: 1000px;
  }

  .login-card {
    background: var(--bg-card);
    border-radius: var(--radius-xl);
    box-shadow: var(--shadow-lg);
    overflow: hidden;
  }

  .login-card__content {
    display: grid;
    grid-template-columns: 1fr;
    min-height: 72vh;
  }

  .login-card__info {
    display: none;
  }

  .login-card__form-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: var(--spacing-lg);
  }

  .login-form-container {
    width: 100%;
    max-width: 400px;
  }

  /* Mobile header */
  .mobile-header {
    text-align: center;
    margin-bottom: var(--spacing-lg);
  }

  .mobile-header__icon {
    width: 64px;
    height: 64px;
    background: var(--gradient-primary);
    border-radius: var(--radius-xl);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 2rem;
    margin-bottom: var(--spacing-md);
    box-shadow: var(--shadow-primary);
  }

  .mobile-header__title {
    font-size: 1.25rem;
    font-weight: bold;
    color: var(--color-gray-800);
    margin-bottom: var(--spacing-xs);
  }

  .mobile-header__subtitle {
    font-size: 0.875rem;
    color: var(--color-gray-500);
    margin: 0;
  }

  /* Form header */
  .login-form-header {
    text-align: center;
    margin-bottom: var(--spacing-lg);
  }

  .login-logo {
    width: 200px;
    height: 200px;
    margin-bottom: var(--spacing-md);
  }

  .login-form-header__title {
    font-size: 1.5rem;
    font-weight: bold;
    color: var(--color-gray-800);
    margin-bottom: var(--spacing-xs);
  }

  .login-form-header__subtitle {
    font-size: 0.875rem;
    color: var(--color-gray-500);
    margin: 0;
  }

  /* Large screens */
  @media (min-width: 992px) {
    .login-card__content {
      grid-template-columns: 1fr 1fr;
    }

    .login-card__info {
      display: flex;
    }

    .login-card__form-wrapper {
      padding: var(--spacing-2xl);
    }

    .mobile-header {
      display: none;
    }
  }

  /* Extra small screens */
  @media (max-width: 576px) {
    .login-page {
      padding: var(--spacing-sm);
    }

    .login-card {
      border-radius: var(--radius-lg);
    }
  }
</style>
