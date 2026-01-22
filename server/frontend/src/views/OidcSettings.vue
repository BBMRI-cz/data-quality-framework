<template>
  <div class="container-fluid py-3 py-md-4">
    <PageHeader
      title="OIDC Settings"
      subtitle="Configure OpenID Connect authentication settings"
    >
      <template #icon>
        <svg class="oidc-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 640">
          <path d="M367.5 496L299.5 528C184.5 517.7 96 456.5 96 382.2C96 310.7 178.5 251.2 287.7 237.9L287.7 280.9C216.2 293.4 163.7 333.9 163.7 382.2C163.7 433.2 222.2 475.5 299.4 485.2L299.4 145.2L367.4 112L367.4 496L367.5 496zM544 355L412.7 326.5L449.5 305.8C430 294.3 406 285.8 379.5 281L379.5 238C425.7 243.5 467.2 257.5 499.8 277.3L534.8 257.5L544 355z"/>
        </svg>
      </template>
    </PageHeader>

    <div class="row justify-content-center">
      <div class="col-12 col-lg-8">

        <!-- OIDC Configuration Card -->
        <div class="card border-0 shadow-sm">
          <div class="card-body p-4">
            <h3 class="h5 fw-bold mb-3">
              <i class="bi bi-gear-fill me-2 text-primary"></i>
              Authentication Configuration
            </h3>
            <p class="text-muted mb-4">
              Configure your OpenID Connect provider settings. Changes will require a page reload to take effect.
            </p>

            <!-- Loading state -->
            <div v-if="loading" class="text-center py-5">
              <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
              </div>
              <p class="text-muted mt-3">Loading settings...</p>
            </div>

            <form v-else @submit.prevent="saveSettings">
              <div class="mb-3">
                <label for="oidcAuthority" class="form-label">
                  OIDC Authority URL
                  <span class="text-danger">*</span>
                </label>
                <input
                  type="url"
                  class="form-control"
                  id="oidcAuthority"
                  v-model="settings.oidcAuthority"
                  required
                  placeholder="https://your-oidc-provider.com"
                />
                <div class="form-text">The URL of your OIDC provider</div>
              </div>

              <div class="mb-3">
                <label for="oidcClientId" class="form-label">
                  Client ID
                  <span class="text-danger">*</span>
                </label>
                <input
                  type="text"
                  class="form-control"
                  id="oidcClientId"
                  v-model="settings.oidcClientId"
                  required
                  placeholder="your-client-id"
                />
                <div class="form-text">The client ID registered with your OIDC provider</div>
              </div>

              <div class="mb-3">
                <label for="oidcRedirectUri" class="form-label">
                  Redirect URI
                  <span class="text-danger">*</span>
                </label>
                <input
                  type="url"
                  class="form-control"
                  id="oidcRedirectUri"
                  v-model="settings.oidcRedirectUri"
                  required
                  placeholder="https://your-app.com/logged-in"
                />
                <div class="form-text">Where users are redirected after login</div>
              </div>

              <div class="mb-3">
                <label for="oidcPostLogoutRedirectUri" class="form-label">
                  Post Logout Redirect URI
                </label>
                <input
                  type="url"
                  class="form-control"
                  id="oidcPostLogoutRedirectUri"
                  v-model="settings.oidcPostLogoutRedirectUri"
                  placeholder="https://your-app.com/login"
                />
                <div class="form-text">Where users are redirected after logout</div>
              </div>

              <div class="mb-3">
                <label for="oidcSilentRedirectUri" class="form-label">
                  Silent Redirect URI
                </label>
                <input
                  type="url"
                  class="form-control"
                  id="oidcSilentRedirectUri"
                  v-model="settings.oidcSilentRedirectUri"
                  placeholder="https://your-app.com/silent-renew"
                />
                <div class="form-text">URI for silent token renewal</div>
              </div>

              <div class="mb-4">
                <label for="oidcScopes" class="form-label">
                  Scopes
                  <span class="text-danger">*</span>
                </label>
                <input
                  type="text"
                  class="form-control"
                  id="oidcScopes"
                  v-model="settings.oidcScopes"
                  required
                  placeholder="openid profile email"
                />
                <div class="form-text">Space-separated list of scopes to request</div>
              </div>

              <div v-if="error" class="alert alert-danger mb-3" role="alert">
                <i class="bi bi-exclamation-triangle me-2"></i>
                {{ error }}
              </div>

              <div class="d-flex gap-2">
                <button
                  type="submit"
                  class="btn btn-primary"
                  :disabled="loading"
                >
                  <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status"></span>
                  {{ loading ? 'Saving...' : 'Save Settings' }}
                </button>
                <button
                  type="button"
                  class="btn btn-outline-secondary"
                  @click="settingsStore.fetchOidcSettings()"
                  :disabled="loading"
                >
                  Reset
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
import { onMounted } from 'vue'
import settingsStore from '../stores/settingsStore'
import { authStore } from '../stores/authStore'
import PageHeader from '../components/PageHeader.vue'
const reloadTimeout = 500
const { settings, loading, error } = settingsStore

async function saveSettings() {
  await settingsStore.updateOidcSettings()

  if (!error.value) {
    if (authStore.mode === 'oidc') {
      authStore.logout()
      setTimeout(() => {
        window.location.href = '/login'
      }, reloadTimeout)
    } else {
      setTimeout(() => {
        window.location.reload()
      }, reloadTimeout)
    }
  }
}

onMounted(async () => {
  await settingsStore.fetchOidcSettings()
})
</script>

<style scoped>
.oidc-icon {
  width: 2rem;
  height: 2rem;
  fill: currentColor;
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
