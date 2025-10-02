import { reactive } from 'vue'

export const authStore = reactive({
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,

  init() {
    const savedUser = localStorage.getItem('user')
    const savedCredentials = localStorage.getItem('authCredentials')

    if (savedUser && savedCredentials) {
      this.user = JSON.parse(savedUser)
      this.isAuthenticated = true
    }
  },

  setUser(user, credentials) {
    this.user = user
    this.isAuthenticated = true
    this.error = null

    localStorage.setItem('user', JSON.stringify(user))
    localStorage.setItem('authCredentials', credentials)
  },

  logout() {
    this.user = null
    this.isAuthenticated = false
    this.error = null

    localStorage.removeItem('user')
    localStorage.removeItem('authCredentials')
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
  }
})

authStore.init()
