const API_BASE = '/api'

class ApiService {
    async login(username, password) {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username,
                password
            })
        })
        if (!response.ok) {
            if (response.status === 401) {
                throw new Error('Invalid credentials')
            }
            throw new Error(`Login failed: ${response.status}`)
        }
        const data = await response.json()
        if (data.token) {
            localStorage.setItem('authToken', data.token)
        }
        return data
    }
}

export const apiService = new ApiService()
