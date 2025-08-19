<template>
  <div class="modal fade" id="patientModal" tabindex="-1" aria-labelledby="patientModalLabel"
      aria-hidden="true" ref="modalElement"
  >
    <div class="modal-dialog modal-lg" role="document">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" id="patientModalLabel">Patient {{ patientId }} Detail</h5>
          <button type="button" class="btn-close" @click="_close" aria-label="Close"></button>
        </div>
        <div class="modal-body">
          <div v-if="patientData">
            <pre style="text-align: left;">
              {{ JSON.stringify(patientData, null, 2) }}
            </pre>
          </div>
          <div v-else>
            Loading patient data for ID: {{ patientId }}...
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="_close">Close</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted, defineProps, defineExpose, watch} from 'vue'
import axios from 'axios'
import { Modal } from 'bootstrap'

const props = defineProps({
  patientId: {
    type: String,
    default: '',
  },
})

let modalElement = ref(null)
let modalInstance = null
const patientData = ref(null)

function _open() {
  if (modalInstance){
    modalInstance.show()
  }
}

function _close() {
  if (modalInstance) {
    modalInstance.hide()
    patientData.value = null
  }
}

watch(() => props.patientId, (newId) => {
  if (newId) {
    fetchPatientData(newId)
  }
})

async function fetchPatientData(patientId) {
  try {
    const response = await axios.get(`api/entities/Patient/${patientId}`)
    patientData.value = response.data
  } catch (error) {
    console.error('Failed to fetch patientData', error)
  }
}

onMounted(() => {
    modalInstance = new Modal(modalElement.value, {
      backdrop: true,
      keyboard: true,
      focus: true,
    })
})

defineExpose({ open: _open,close: _close })
</script>