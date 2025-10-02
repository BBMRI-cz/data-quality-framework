import { reactive } from 'vue'

export const authStore = reactive({
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
  redirectPath: null,

  init() {
    const savedUser = localStorage.getItem('user')
    const savedToken = localStorage.getItem('authToken')

    if (savedUser && savedToken) {
      this.user = JSON.parse(savedUser)
      this.isAuthenticated = true
    }
  },

  setUser(user, token) {
    this.user = user
    this.isAuthenticated = true
    this.error = null

    localStorage.setItem('user', JSON.stringify(user))
    if (token) {
      localStorage.setItem('authToken', token)
    }
  },

  logout() {
    this.user = null
    this.isAuthenticated = false
    this.error = null
    this.redirectPath = null

    localStorage.removeItem('user')
    localStorage.removeItem('authToken')
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
  }
})

authStore.init()
