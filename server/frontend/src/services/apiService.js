const API_BASE = '/api'

class ApiService {
  async request(endpoint, options = {}) {
    const url = `${API_BASE}${endpoint}`
    const credentials = localStorage.getItem('authCredentials')

    const config = {
      headers: {
        'Content-Type': 'application/json',
        ...(credentials && { 'Authorization': `Basic ${credentials}` }),
        ...options.headers
      },
      ...options
    }

    try {
      const response = await fetch(url, config)

      if (!response.ok) {
        if (response.status === 401) {
          localStorage.removeItem('authCredentials')
          localStorage.removeItem('user')
          throw new Error('Authentication failed')
        }
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      return await response.json()
    } catch (error) {
      throw new Error(`API request failed: ${error.message}`)
    }
  }

  async login(username, password) {
    const credentials = btoa(`${username}:${password}`)

    return this.request('/auth/login', {
      method: 'GET',
      headers: {
        'Authorization': `Basic ${credentials}`
      }
    })
  }

  async getCurrentUser() {
    return this.request('/auth/login', {
      method: 'GET'
    })
  }
}

export const apiService = new ApiService()
