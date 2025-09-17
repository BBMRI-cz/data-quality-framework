<template>
  <div class="card mb-4">
    <!-- Health Status Banner -->
    <div v-if="healthStore.healthStatus"
         :class="['alert', 'mb-0', healthStore.healthStatus.status === 'UP' ? 'alert-success' : 'alert-danger']">
      <div class="d-flex justify-content-between align-items-center">
        <span>
          <i :class="['bi', healthStore.healthStatus.status === 'UP' ? 'bi-check-circle' : 'bi-exclamation-triangle']"></i>
          FHIR Server: {{ healthStore.healthStatus.status }}
          <span v-if="healthStore.healthStatus.details?.error"> - {{ healthStore.healthStatus.details.error }}</span>
        </span>
        <button class="btn btn-sm btn-outline-secondary"
                @click="healthStore.checkHealth()"
                :disabled="healthStore.isChecking">
          <span v-if="healthStore.isChecking" class="spinner-border spinner-border-sm me-1"></span>
          Refresh
        </button>
      </div>
    </div>
    <div class="card-header d-flex justify-content-between align-items-center">
      <h2 class="mb-0">Data Quality Reports</h2>
      <button
          class="btn btn-success"
          @click="generateReportWithReset"
          :disabled="reportStore.isGenerating || healthStore.healthStatus?.status !== 'UP'"
      >
        <span v-if="reportStore.isGenerating" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
        {{ reportStore.isGenerating ? 'Generating...' : 'Generate Report' }}
      </button>
    </div>

    <div class="card-body">
      <div v-if="reportStore.reports.length === 0 && !reportStore.isGenerating" class="text-muted">
        No reports available. Please dataQualityCheck back later.
      </div>

      <div v-else-if="latestReport">
        <!-- Latest Report -->
        <div class="mb-4">
          <h3 class="h5">Latest Report (ID: {{ latestReport.id }})</h3>
          <div class="card">
            <div class="card-body">
              <div class="d-flex gap-3">
                <p><strong>Generated At:</strong> {{ formatDate(latestReport.generatedAt) }}</p>
                <div v-if="isOverBudget()" class="warning">
                  <span class="warning-icon">
                    <i class="bi bi-exclamation-triangle"></i>
                  </span>
                  Epsilon budget exceeded! Total epsilon: {{ calculateEpsilonUsed().toFixed(2) }} (Budget: {{ latestReport.epsilonBudget.toFixed(2) }})
                </div>
                <div v-else>
                  Total epsilon: {{ calculateEpsilonUsed().toFixed(2) }} (Budget: {{ latestReport.epsilonBudget.toFixed(2) }})
                </div>
                <p v-if="showValues"><strong>Total Entities:</strong> {{ latestReport.numberOfEntities }}</p>
              </div>
              <div class="d-flex justify-content-between align-items-center mt-3">
                <h4 class="h6 mb-0">Results</h4>
                <button
                    class="btn btn-sm btn-outline-secondary"
                    @click="toggleValues"
                    :title="showValues ? 'Hide values' : 'Show values'"
                >
                  <i :class="['bi', showValues ? 'bi-eye-slash' : 'bi-eye']"></i>
                </button>
              </div>
              <div class="d-flex flex-column gap-3 mt-2">
                <div
                    v-for="result in latestReport.results"
                    :key="getCheckIdKey(result)"
                    :class="['card', getResultClass(result)]"
                >
                  <div class="card-body p-2 text-start">
                    <div class="d-flex justify-content-between align-items-center">
                      <div>
                        <h5 class="card-title fs-6">{{ result.checkName }}</h5>
                        <p class="card-text mb-1"><strong>Epsilon Used:</strong> {{ result.epsilon }}</p>
                        <p class="card-text mb-1"v-if="showValues"><strong>Raw value</strong> {{ result.rawValue }}</p>
                        <p class="card-text mb-0"><strong>Occurrence rate:</strong> {{ calculatePercentage(result.obfuscatedValue) }}%</p>
                        <p class="card-text mb-1" v-if="result.error"><strong>Error message:</strong> {{ result.error }}</p>
                      </div>
                      <button
                          v-if="Array.isArray(result.patients)
                               && result.patients.length > 0"
                          class="btn btn-sm btn-outline-secondary"
                          @click="toggleIds(getCheckIdKey(result))"
                          :title="openIds[getCheckIdKey(result)] ? 'Hide Ids' : 'Show Ids'"
                      >
                        <i class="bi bi-person-lines-fill"></i>
                      </button>
                    </div>
                    <div
                        v-if="Array.isArray(result.patients)
                               && result.patients.length > 0
                               && openIds[getCheckIdKey(result)]">
                      <p class="card-text mb-1 mt-1"><strong>Patient Identifiers:</strong></p>
                      <table class="table table-sm table-inner-only table-striped text-center w-100 rounded-3 overflow-hidden"
                        style="opacity: 90%">
                        <tbody>
                        <tr v-for="(row, rowIndex) in patientTableRows(result)" :key="rowIndex">
                          <td v-for="(patient, colIndex) in row" :key="colIndex" style="max-width: 16.6%">
                            <a href="#"
                               v-if="patient"
                               @click.prevent="showPatientDetail(patient)"
                               target="_blank">
                              {{ patient}}
                            </a>
                          </td>
                        </tr>
                        </tbody>
                      </table>
                      <nav class="d-flex justify-content-center"
                          v-if="result.patients.length > pageSize"
                      >
                        <Pagination :current-page="idPage[getCheckIdKey(result)] || 1"
                                    :page-size="pageSize"
                                    :total-pages="Math.ceil((result.patients?.length || 0) / pageSize)"
                                    :max-visible-buttons="5"
                                    @page-changed="page => changePage(getCheckIdKey(result), page)"
                        ></Pagination>
                      </nav>
                    </div>
                  </div>
                </div>
                <PatientModal ref="patientModalRef" :patient-id="modalPatientId"></PatientModal>
              </div>
              <p class="mt-3">
                <strong>Link:</strong>
                <a :href="latestReport._links.self.href" target="_blank">View Report</a>
              </p>
            </div>
          </div>
        </div>

        <!-- Other Reports -->
        <div v-if="otherReports.length > 0">
          <h3 class="h5">Other Reports</h3>
          <ul class="list-group">
            <li
                v-for="report in otherReports"
                :key="report.id"
                class="list-group-item d-flex justify-content-between align-items-center"
            >
              <div>
                <strong>Report ID: {{ report.id }}</strong> - Generated: {{ formatDate(report.generatedAt) }} (Status: {{ report.status }})
              </div>
              <a :href="report._links.self.href" target="_blank" class="btn btn-sm btn-outline-primary">View</a>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import PatientModal from "./PatientModal.vue";
import Pagination from "./Pagination.vue";
import reportStore from '../stores/reportStore.js'
import healthStore from '../stores/healthStore.js'


const showValues = ref(true) // Reactive state for toggling visibility
const openIds = ref({})
const pageSize = 60
const idPage = ref({})
const patientModalRef = ref(null)
const modalPatientId = ref('')


const toggleValues = () => {
  showValues.value = !showValues.value
}

function toggleIds(checkId) {
  openIds.value[checkId] = !openIds.value[checkId]
}

function changePage(checkId, page) {
  idPage.value[checkId] = page;
}

function paginatedPatients(result) {
  const all = result.patients || [];
  const currentPage = idPage.value[getCheckIdKey(result)] || 1;
  const start = (currentPage - 1) * pageSize;
  return all.slice(start, start + pageSize);
}

function patientTableRows(result) {
  const patients = paginatedPatients(result);
  const rows = [];
  for (let i = 0; i < patients.length; i += 6) {
    rows.push(patients.slice(i, i + 6));
  }
  return rows;
}

function showPatientDetail(patient) {
  modalPatientId.value = patient
  patientModalRef.value.open()
}

function getCheckIdKey(result) {
  return result.checkId + '_' + (result.stratum || 'all')
}

const latestReport = computed(() => {
  if (!reportStore.reports.length) return null
  return [...reportStore.reports].sort((a, b) => new Date(b.generatedAt) - new Date(a.generatedAt))[0]
})

const otherReports = computed(() => {
  if (reportStore.reports.length <= 1) return []
  return [...reportStore.reports]
      .sort((a, b) => new Date(b.generatedAt) - new Date(a.generatedAt))
      .slice(1)
})

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

const calculatePercentage = (value) => {
  const total = latestReport.value?.numberOfEntities || 1
  return ((value / total) * 100).toFixed(2)
}

const calculateEpsilonUsed = () => {
  return latestReport.value.results.reduce((sum, result) => sum + result.epsilon, 0)
}

const isOverBudget = () => {
  return calculateEpsilonUsed() > latestReport.value.epsilonBudget
}

const getResultClass = (result) => {
  const percentage = parseFloat(calculatePercentage(result.obfuscatedValue))
  if (percentage >= result.errorThreshold || result.error) {
    return 'bg-danger bg-opacity-25'
  } else if (percentage >= result.warningThreshold) {
    return 'bg-warning bg-opacity-25'
  }
  return 'bg-success bg-opacity-25'
}

const generateReportWithReset = async () => {
  await reportStore.generateReport()
  openIds.value = {}
  idPage.value = {}
}

const checkServer = async () => {

  await Promise.all([
    reportStore.fetchReports(),
    healthStore.checkHealth()
  ])
}

onMounted(() => {
  checkServer()
})

</script>

<style scoped>
.card.bg-warning.bg-opacity-25,
.card.bg-danger.bg-opacity-25,
.card.bg-success.bg-opacity-25 {
  width: 100%;
  font-size: 0.9rem;
}
.gap-3 {
  gap: 1rem;
}
.bi-exclamation-triangle {
  color: red;
}
.text-start {
  text-align: left !important;
}

.table-inner-only {
  border-collapse: collapse;
  border: none;
}

.table-inner-only td {
  border: none;
}

.table-inner-only td + td {
  border-left: 1px solid #dee2e6;
}

.table-inner-only tr + tr td {
  border-top: 1px solid #dee2e6;
}

</style>