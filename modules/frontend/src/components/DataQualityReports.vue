<template>
  <div class="card mb-4">
    <div class="card-header d-flex justify-content-between align-items-center">
      <h2 class="mb-0">Data Quality Reports</h2>
      <button
          class="btn btn-success"
          @click="generateReport"
          :disabled="isGenerating"
      >
        <span v-if="isGenerating" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
        {{ isGenerating ? 'Generating...' : 'Generate Report' }}
      </button>
    </div>

    <div class="card-body">
      <div v-if="reports.length === 0 && !isGenerating" class="text-muted">
        No reports available. Please check back later.
      </div>

      <div v-else-if="latestReport">
        <!-- Latest Report -->
        <div class="mb-4">
          <h3 class="h5">Latest Report (ID: {{ latestReport.id }})</h3>
          <div class="card">
            <div class="card-body">
              <div class="d-flex gap-3" >
                <p><strong>Generated At:</strong> {{ formatDate(latestReport.generatedAt) }}</p>
<!--                <p><strong>Status:</strong> {{ latestReport.status }}</p>-->
                <div v-if="isOverBudget" class="warning">
                  <span class="warning-icon">
                    <i class="bi bi-exclamation-triangle"></i>
                  </span>
                  Epsilon budget exceeded! Total epsilon: {{ calculateEpsilonUsed().toFixed(2) }} (Budget: {{ latestReport.epsilonBudget.toFixed(2) }})
                </div>
                <p><strong>Total Entities:</strong> {{ latestReport.numberOfEntities }}</p>
              </div>
              <h4 class="h6 mt-3">Results</h4>
              <div class="d-flex flex-column gap-3">
                <div
                    v-for="result in latestReport.results"
                    :key="result.checkId"
                    :class="['card', getResultClass(result)]"
                >
                  <div class="card-body p-2 text-start">
                    <h5 class="card-title fs-6">{{ result.checkName }}</h5>
                    <p class="card-text mb-1"><strong>Raw Value:</strong> {{ result.error }}</p>
                    <p class="card-text mb-1"><strong>Epsilon Used:</strong> {{ result.epsilon }}</p>
                    <p class="card-text mb-0"><strong>Error rate:</strong> {{ calculatePercentage(result.obfuscatedValue) }}%</p>
                  </div>
                </div>
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
import axios from 'axios'

const reports = ref([])
const isGenerating = ref(false)

const fetchReports = async () => {
  try {
    const { data } = await axios.get('/api/reports')
    reports.value = data._embedded?.reports || []
  } catch (error) {
    console.error('Error fetching reports:', error)
    reports.value = []
  }
}

const generateReport = async () => {
  isGenerating.value = true
  try {
    const { data } = await axios.post('/api/reports', {})
    const reportUrl = data._links.self.href
    let report = data
    while (report.status === 'GENERATING') {
      await new Promise(resolve => setTimeout(resolve, 2000))
      const poll = await axios.get(reportUrl)
      report = poll.data
    }
    await fetchReports()
  } catch (error) {
    console.error('Error generating report:', error)
  } finally {
    isGenerating.value = false
  }
}

const latestReport = computed(() => {
  if (reports.value.length === 0) return null
  return [...reports.value].sort((a, b) => new Date(b.generatedAt) - new Date(a.generatedAt))[0]
})

const otherReports = computed(() => {
  if (reports.value.length <= 1) return []
  return [...reports.value]
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

// Computes percentage for a result
const calculatePercentage = (value) => {
  const total = latestReport.value?.numberOfEntities || 1
  return ((value / total) * 100).toFixed(2)
}
const calculateEpsilonUsed = (value) => {
  return latestReport.value.results.reduce((sum, result) => sum + result.epsilon, 0);
}
const isOverBudget = (value) => {
  return calculateEpsilonUsed() > latestReport.value.epsilonBudget;
}
// Determines card class based on thresholds
const getResultClass = (result) => {
  const percentage = parseFloat(calculatePercentage(result.obfuscatedValue))
  if (percentage >= result.errorThreshold || result.error) {
    return 'bg-danger bg-opacity-25'
  } else if (percentage >= result.warningThreshold) {
    return 'bg-warning bg-opacity-25'
  }
  return 'bg-success bg-opacity-25'
}

onMounted(fetchReports)
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
.bi-exclamation-triangle{
  color: red;
}
.text-start {
  text-align: left !important;
}
</style>
