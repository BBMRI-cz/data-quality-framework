import { reactive } from 'vue'
import { api } from '../js/api.js'

const store = reactive({
    healthStatus: null,
    isChecking: false,
    lastChecked: null,

    async checkHealth() {
        store.isChecking = true
        try {
            const { data } = await api.get('/api/entities/health')
            store.healthStatus = data
            store.lastChecked = new Date()
        } catch (err) {
            console.error('Health check failed:', err)
            store.healthStatus = {
                status: 'DOWN',
                details: {
                    error: err.response?.data?.details?.error || err.message || 'Connection failed'
                }
            }
            store.lastChecked = new Date()
        } finally {
            store.isChecking = false
        }
    }
})

export default store