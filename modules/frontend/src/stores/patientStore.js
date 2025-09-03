import { ref } from 'vue'
import { api } from '../js/api'

const patientData = ref(null)

async function fetchPatientData(patientId) {
    try {
        const response = await api.get(`api/entities/Patient/${patientId}`)
        patientData.value = response.data
        return response.data
    } catch (error) {
        console.error('Failed to fetch patientData', error)
        throw error
    }
}

export default {
    patientData,
    fetchPatientData,
}
