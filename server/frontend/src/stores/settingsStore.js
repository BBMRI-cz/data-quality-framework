import { ref } from 'vue'
import api from '../services/api'

const settings = ref({
  oidcAuthority: '',
  oidcClientId: '',
  oidcRedirectUri: '',
  oidcPostLogoutRedirectUri: '',
  oidcScopes: '',
  oidcSilentRedirectUri: ''
})
const loading = ref(false)
const error = ref(null)

async function fetchOidcSettings() {
  loading.value = true
  error.value = null
  try {
    const response = await api.get('/v1/settings/oidc')
    settings.value = response.data
  } catch (err) {
    error.value = 'Failed to load settings. Please try again.'
    console.error('Failed to load settings:', err)
  } finally {
    loading.value = false
  }
}

async function updateOidcSettings() {
  loading.value = true
  error.value = null

  try {
    await api.patch('/v1/settings/oidc', settings.value)
  } catch (err) {
    error.value = err.response?.data?.message || 'Failed to save settings. Please try again.'
    console.error('Failed to save settings:', err)
  } finally {
    loading.value = false
  }
}

export default {
  settings,
  loading,
  error,
  fetchOidcSettings,
  updateOidcSettings
}



