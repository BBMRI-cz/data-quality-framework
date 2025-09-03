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
          <div v-if="patientStore.patientData">
            <pre style="text-align: left;">
              {{ patientStore.patientData}}
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
import { Modal } from 'bootstrap'
import patientStore from '../stores/patientStore.js'

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
    patientStore.patientData.value = null
  }
}

watch(() => props.patientId, (newId) => {
  if (newId) {
    patientStore.patientData.value = null
    patientStore.fetchPatientData(newId)
  }
})

onMounted(() => {
    modalInstance = new Modal(modalElement.value, {
      backdrop: true,
      keyboard: true,
      focus: true,
    })
})

defineExpose({ open: _open,close: _close })
</script>