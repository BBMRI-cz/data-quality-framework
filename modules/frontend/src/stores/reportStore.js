import { reactive } from 'vue'
import { api } from '../js/api.js'

const store = reactive({
    reports: [],
    isGenerating: false,
    async fetchReports() {
        try {
            const { data } = await api.get('/api/reports')
            store.reports = data._embedded?.reports || []
        } catch (err) {
            console.error(err)
            store.reports = []
        }
    },
    async generateReport() {
        store.isGenerating = true
        try {
            const { data } = await api.post('/api/reports', {})
            let report = data
            const reportUrl = report._links.self.href

            while (report.status === 'GENERATING') {
                await new Promise(r => setTimeout(r, 2000))
                const poll = await api.get(reportUrl)
                report = poll.data
            }

            await store.fetchReports()
        } catch (err) {
            console.error(err)
        } finally {
            store.isGenerating = false
        }
    }
})

export default store
