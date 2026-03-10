<template>
  <div class="container-fluid py-3 py-md-4">
    <PageHeader title="OIDC Settings" subtitle="Configure OpenID Connect authentication settings">
      <template #icon>
        <svg class="oidc-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 640">
          <path
            d="M367.5 496L299.5 528C184.5 517.7 96 456.5 96 382.2C96 310.7 178.5 251.2 287.7 237.9L287.7 280.9C216.2 293.4 163.7 333.9 163.7 382.2C163.7 433.2 222.2 475.5 299.4 485.2L299.4 145.2L367.4 112L367.4 496L367.5 496zM544 355L412.7 326.5L449.5 305.8C430 294.3 406 285.8 379.5 281L379.5 238C425.7 243.5 467.2 257.5 499.8 277.3L534.8 257.5L544 355z"
          />
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
              Configure your OpenID Connect provider settings. Changes will require a page reload to
              take effect.
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
                  id="oidcAuthority"
                  v-model="settings.oidcAuthority"
                  type="url"
                  class="form-control"
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
                  id="oidcClientId"
                  v-model="settings.oidcClientId"
                  type="text"
                  class="form-control"
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
                  id="oidcRedirectUri"
                  v-model="settings.oidcRedirectUri"
                  type="url"
                  class="form-control"
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
                  id="oidcPostLogoutRedirectUri"
                  v-model="settings.oidcPostLogoutRedirectUri"
                  type="url"
                  class="form-control"
                  placeholder="https://your-app.com/login"
                />
                <div class="form-text">Where users are redirected after logout</div>
              </div>

              <div class="mb-3">
                <label for="oidcAuthorityName" class="form-label"> Authority Display Name </label>
                <input
                  id="oidcAuthorityName"
                  v-model="settings.oidcAuthorityName"
                  type="text"
                  class="form-control"
                  placeholder="BBMRI Identity Provider"
                />
                <div class="form-text">Name shown to users during authentication flows</div>
              </div>

              <div class="mb-3">
                <label for="oidcAuthorityLogo" class="form-label"> Authority Logo URL </label>
                <input
                  id="oidcAuthorityLogo"
                  v-model="settings.oidcAuthorityLogo"
                  type="text"
                  class="form-control"
                  placeholder="https://your-oidc-provider.com/logo.svg"
                />
                <div class="form-text">Logo displayed alongside the authority name</div>
              </div>

              <div class="mb-3">
                <label for="oidcScopes" class="form-label">
                  Scopes
                  <span class="text-danger">*</span>
                </label>
                <input
                  id="oidcScopes"
                  v-model="settings.oidcScopes"
                  type="text"
                  class="form-control"
                  required
                  placeholder="openid profile email"
                />
                <div class="form-text">Space-separated list of scopes to request</div>
              </div>

              <div class="mb-4">
                <label for="oidcSwaggerRedirectUrl" class="form-label">
                  Swagger OAuth Redirect URL
                </label>
                <input
                  id="oidcSwaggerRedirectUrl"
                  v-model="settings.oidcSwaggerRedirectUrl"
                  type="url"
                  class="form-control"
                  placeholder="http://localhost:8082/api/swagger-ui/oauth2-redirect.html"
                />
                <div class="form-text">
                  OAuth2 redirect URL for Swagger UI documentation (e.g.,
                  http://localhost:8082/api/swagger-ui/oauth2-redirect.html for Docker,
                  https://your-domain.com/api/swagger-ui/oauth2-redirect.html for production)
                </div>
              </div>

              <div v-if="error" class="alert alert-danger mb-3" role="alert">
                <i class="bi bi-exclamation-triangle me-2"></i>
                {{ error }}
              </div>

              <div class="d-flex gap-2 flex-wrap">
                <button type="submit" class="btn btn-primary" :disabled="loading">
                  <span
                    v-if="loading"
                    class="spinner-border spinner-border-sm me-2"
                    role="status"
                  ></span>
                  {{ loading ? 'Saving...' : 'Save Settings' }}
                </button>
                <button
                  type="button"
                  class="btn btn-outline-secondary"
                  :disabled="loading"
                  @click="settingsStore.fetchOidcSettings()"
                >
                  Reset
                </button>
                <button
                  type="button"
                  class="btn btn-outline-danger"
                  :disabled="loading"
                  @click="showRemoveModal = true"
                >
                  <i class="bi bi-trash me-1"></i>
                  Remove OIDC Config
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>

    <!-- Remove OIDC Config Confirmation Modal -->
    <DeleteConfirmModal
      :show="showRemoveModal"
      title="Remove OIDC Configuration"
      subtitle="This action will disable OIDC authentication"
      message="Are you sure you want to remove the OIDC configuration? This will clear all OIDC settings and may affect user authentication."
      warning="You will be logged out and redirected to the login page."
      confirm-text="Remove Configuration"
      :loading="loading"
      @close="showRemoveModal = false"
      @confirm="removeOidcConfig"
    />
  </div>
</template>

<script setup>
  import { onMounted, ref } from 'vue';
  import settingsStore from '../stores/settingsStore';
  import { authStore } from '../stores/authStore';
  import PageHeader from '../components/PageHeader.vue';
  import DeleteConfirmModal from '../components/DeleteConfirmModal.vue';

  const reloadTimeout = 500;
  const { settings, loading, error } = settingsStore;
  const showRemoveModal = ref(false);

  async function saveSettings() {
    settings.value.oidcAuthority =
      settings.value.oidcAuthority?.trim() || '';
    settings.value.oidcRedirectUri =
      settings.value.oidcRedirectUri?.trim().replace(/\/+$/, '') || '';
    settings.value.oidcPostLogoutRedirectUri =
      settings.value.oidcPostLogoutRedirectUri?.trim().replace(/\/+$/, '') || '';
    settings.value.oidcSwaggerRedirectUrl =
      settings.value.oidcSwaggerRedirectUrl?.trim().replace(/\/+$/, '') || '';

    await settingsStore.updateOidcSettings();

    async function saveSettings() {
      if (!error.value) {
        if (authStore.mode === 'oidc') {
          authStore.logout();
          setTimeout(() => {
            window.location.href = '/login';
          }, reloadTimeout);
        } else {
          setTimeout(() => {
            window.location.reload();
          }, reloadTimeout);
        }
      }
    }

    async function removeOidcConfig() {
      settings.value.oidcAuthority = '';
      settings.value.oidcClientId = '';
      settings.value.oidcRedirectUri = '';
      settings.value.oidcPostLogoutRedirectUri = '';
      settings.value.oidcAuthorityName = '';
      settings.value.oidcAuthorityLogo = '';
      settings.value.oidcScopes = '';
      settings.value.oidcSwaggerRedirectUrl = '';

      await settingsStore.updateOidcSettings();
      showRemoveModal.value = false;

      if (!error.value) {
        if (authStore.mode === 'oidc') {
          authStore.logout();
          setTimeout(() => {
            window.location.href = '/login';
          }, reloadTimeout);
        } else {
          setTimeout(() => {
            window.location.reload();
          }, reloadTimeout);
        }
      }
    }

    onMounted(async () => {
      await settingsStore.fetchOidcSettings();
    });
  }
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
