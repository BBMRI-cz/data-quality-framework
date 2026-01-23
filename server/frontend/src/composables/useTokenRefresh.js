import { ref, onUnmounted } from 'vue'
import { useOidcStore } from 'vue3-oidc'
import { authStore } from '../stores/authStore'

const REFRESH_BEFORE_EXPIRY_MS = 60000 // Refresh 1 minute before expiry
const CHECK_INTERVAL_MS = 30000 // Check every 30 seconds

export function useTokenRefresh() {
  const isRefreshing = ref(false)
  let intervalId = null

  const refreshToken = async () => {
    if (isRefreshing.value) {
      console.log('Token refresh already in progress')
      return false
    }

    try {
      isRefreshing.value = true
      const oidcStore = useOidcStore()
      const userManager = oidcStore.state.value?.userManager

      if (!userManager) {
        console.error('UserManager not available for token refresh')
        return false
      }

      const currentUser = await userManager.getUser()

      if (!currentUser) {
        console.warn('No user found for token refresh')
        return false
      }

      if (!currentUser.refresh_token) {
        console.warn('No refresh token available')
        return false
      }

      console.log('Refreshing access token...')
      const newUser = await userManager.signinSilent()

      if (newUser && newUser.access_token) {
        console.log('Token refreshed successfully')
        authStore.setUser(newUser, newUser.access_token, 'oidc')
        return true
      }

      console.warn('Token refresh did not return new user')
      return false
    } catch (error) {
      console.error('Token refresh failed:', error)
      // If refresh fails, the user might need to re-authenticate
      if (error.error === 'login_required' || error.error === 'invalid_grant') {
        console.error('User must re-authenticate')
        authStore.logout()
      }
      return false
    } finally {
      isRefreshing.value = false
    }
  }

  const shouldRefreshToken = (user) => {
    if (!user || !user.expires_at) {
      return false
    }

    const expiresAt = user.expires_at * 1000 // Convert to milliseconds
    const now = Date.now()
    const timeUntilExpiry = expiresAt - now

    return timeUntilExpiry > 0 && timeUntilExpiry <= REFRESH_BEFORE_EXPIRY_MS
  }

  const checkAndRefreshToken = async () => {
    try {
      const oidcStore = useOidcStore()
      const userManager = oidcStore.state.value?.userManager

      if (!userManager) {
        return
      }

      const user = await userManager.getUser()

      if (!user) {
        return
      }

      // Check if token is about to expire
      if (shouldRefreshToken(user)) {
        console.log('Token expiring soon, refreshing...')
        await refreshToken()
      }
    } catch (error) {
      console.error('Error checking token expiry:', error)
    }
  }

  const startTokenRefreshCheck = () => {
    if (intervalId !== null) {
      console.log('Token refresh check already running')
      return
    }

    console.log('Starting token refresh check')
    // Check immediately on start
    checkAndRefreshToken()

    // Then check periodically
    intervalId = setInterval(checkAndRefreshToken, CHECK_INTERVAL_MS)
  }

  const stopTokenRefreshCheck = () => {
    if (intervalId !== null) {
      console.log('Stopping token refresh check')
      clearInterval(intervalId)
      intervalId = null
    }
  }

  onUnmounted(() => {
    stopTokenRefreshCheck()
  })

  return {
    refreshToken,
    isRefreshing,
    startTokenRefreshCheck,
    stopTokenRefreshCheck
  }
}
