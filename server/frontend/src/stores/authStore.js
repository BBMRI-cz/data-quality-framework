import { reactive } from 'vue'
import { apiService } from '../services/apiService'
import { resetOidc } from '../utils/oidc'

export const authStore = reactive({
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
  redirectPath: null,
  mode: null,

  init() {
    const savedUser = localStorage.getItem('user')
    const savedToken = localStorage.getItem('authToken')
    const savedMode = localStorage.getItem('authMode')

    if (savedUser && savedToken) {
      this.user = JSON.parse(savedUser)
      this.isAuthenticated = true
      this.mode = savedMode || 'basic'
    }
  },

  async setUser(user, token, mode = 'basic') {
    this.user = user
    this.isAuthenticated = true
    this.error = null
    this.mode = mode

    localStorage.setItem('authMode', mode)
    if (token) {
      localStorage.setItem('authToken', token)
    }

    if (mode === 'oidc') {
      await this.mergeUserData()
    } else {
      localStorage.setItem('user', JSON.stringify(user))
    }
  },

  logout() {
    this.user = null
    this.isAuthenticated = false
    this.error = null
    this.redirectPath = null
    this.mode = null

    localStorage.removeItem('user')
    localStorage.removeItem('authToken')
    localStorage.removeItem('authMode')
    localStorage.removeItem('rememberedUsername')

    resetOidc()
  },

  setLoading(loading) {
    this.isLoading = loading
  },

  setError(error) {
    this.error = error
  },

  clearError() {
    this.error = null
  },

  setRedirectPath(path) {
    this.redirectPath = path
  },

  getRedirectPath() {
    const path = this.redirectPath
    this.redirectPath = null // Clear after getting
    return path || '/dashboard'
  },

  async mergeUserData() {
    if (!this.user) return

    this.setLoading(true)
    try {
      const userData = await apiService.getUserProfile()
      this.user = {
        ...this.user,
        id: userData.id,
        agentId: userData.agentId,
        roles: userData.roles,
        username: userData.username
      }
    } catch (error) {
      console.error('Failed to fetch user profile:', error)
      this.setError('Failed to load user profile data')
    } finally {
      localStorage.setItem('user', JSON.stringify(this.user))
      this.setLoading(false)
    }
  }

})

authStore.init()
