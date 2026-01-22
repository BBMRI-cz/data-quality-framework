import { reactive } from 'vue'

export const authStore = reactive({
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
  redirectPath: null,
  mode: null,
  silentRenewFailed: false,

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

  setUser(user, token, mode = 'basic') {
    this.user = user
    this.isAuthenticated = true
    this.error = null
    this.mode = mode
    this.silentRenewFailed = false

    localStorage.setItem('user', JSON.stringify(user))
    localStorage.setItem('authMode', mode)
    if (token) {
      localStorage.setItem('authToken', token)
    }
  },

  logout() {
    this.user = null
    this.isAuthenticated = false
    this.error = null
    this.redirectPath = null
    this.mode = null
    this.silentRenewFailed = false

    localStorage.removeItem('user')
    localStorage.removeItem('authToken')
    localStorage.removeItem('authMode')
    localStorage.removeItem('rememberedUsername')
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

  setSilentRenewFailed(failed) {
    this.silentRenewFailed = failed
  }
})

authStore.init()
