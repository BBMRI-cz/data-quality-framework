<template>
  <div class="container-fluid px-2 px-md-3 py-3 py-md-4">
    <div class="row">
      <div class="col-12">
        <!-- Page Header -->
        <PageHeader
          title="Reports"
          mobile-title="Reports"
          subtitle="View and analyze data quality reports from all agents"
          icon="bi bi-file-earmark-text-fill"
        >
          <template #actions>
            <button
              @click="fetchData"
              class="btn btn-outline-primary btn-sm"
              :disabled="loading"
            >
              <i class="bi bi-arrow-clockwise"></i>
              <span class="d-none d-md-inline ms-1">Refresh</span>
            </button>
          </template>
        </PageHeader>

        <!-- Stats Cards -->
        <div class="stats-grid mb-3 mb-md-4">
          <div class="stat-card">
            <div class="stat-number text-dark">{{ reports.length }}</div>
            <div class="stat-label">Total Reports</div>
          </div>
          <div class="stat-card">
            <div class="stat-number text-success">{{ reportStats.passed }}</div>
            <div class="stat-label">Passed</div>
          </div>
          <div class="stat-card">
            <div class="stat-number text-warning">{{ reportStats.warnings }}</div>
            <div class="stat-label">With Warnings</div>
          </div>
          <div class="stat-card">
            <div class="stat-number text-danger">{{ reportStats.failed }}</div>
            <div class="stat-label">Failed</div>
          </div>
        </div>

        <!-- Loading state -->
        <div v-if="loading" class="loading-state">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
        </div>

        <!-- Error state -->
        <div v-else-if="error" class="alert alert-danger" role="alert">
          <i class="bi bi-exclamation-triangle-fill me-2"></i>
          {{ error }}
        </div>

        <!-- Reports table -->
        <div v-else>
          <div class="mb-4">
            <div class="filter-label">Status:</div>
            <CategoryFilter
                :categories="statuses"
                v-model="selectedStatus"
            />
          </div>
          <ReportsTable
              :reports="reports"
              :quality-check-map="qualityCheckMap"
              :agents="agents"
              :selected-status="selectedStatus"
          />
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import ReportsTable from '../components/ReportsTable.vue'
import PageHeader from '../components/PageHeader.vue'
import CategoryFilter from '../components/CategoryFilter.vue'
import { apiService } from '../services/apiService.js'
import { getReportStatus, CheckStatus } from '../utils/qualityCheckUtils.js'

const reports = ref([])
const qualityCheckMap = ref(new Map())
const agents = ref([])
const loading = ref(true)
const error = ref(null)

const selectedStatus = ref(null)
const statuses = [CheckStatus.PASSED, CheckStatus.WARNING, CheckStatus.FAILED]

const reportStats = computed(() => {
  const stats = {
    passed: 0,
    warnings: 0,
    failed: 0
  }

  reports.value.forEach(report => {
    const status = getReportStatus(report, qualityCheckMap.value)

    switch (status) {
      case CheckStatus.PASSED:
        stats.passed++
        break
      case CheckStatus.WARNING:
        stats.warnings++
        break
      case CheckStatus.FAILED:
        stats.failed++
        break
    }
  })

  return stats
})

const fetchData = async () => {
  try {
    loading.value = true
    error.value = null

    // Fetch quality checks, reports, and agents in parallel
    const [checksData, reportsData, agentsData] = await Promise.all([
      apiService.getQualityChecks(),
      apiService.getReports(),
      apiService.getAgents()
    ])

    // Handle HAL format response for quality checks
    const checks = checksData._embedded?.qualityChecks || (Array.isArray(checksData) ? checksData : [])

    // Handle HAL format response for reports
    const reportsArray = reportsData._embedded?.reports || (Array.isArray(reportsData) ? reportsData : [])

    // Handle HAL format response for agents
    agents.value = agentsData._embedded?.agents || (Array.isArray(agentsData) ? agentsData : [])

    // Convert quality checks array to Map for quick lookup
    qualityCheckMap.value = new Map(checks.map(check => [check.hash, check]))

    // Sort reports by timestamp (newest first)
    reports.value = reportsArray.sort((a, b) =>
      new Date(b.timestamp) - new Date(a.timestamp)
    )
  } catch (err) {
    console.error('Error fetching reports:', err)
    error.value = err.message || 'Failed to load reports'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 0.75rem;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.stat-number {
  font-size: 1.75rem;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 0.25rem;
}

.stat-label {
  font-size: 0.813rem;
  color: #6c757d;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.spinner-border {
  width: 3rem;
  height: 3rem;
}

/* Filter Labels */
.filter-label {
  font-size: 0.875rem;
  color: #6c757d;
  font-weight: 500;
  margin-bottom: 0.5rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

@media (min-width: 576px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 1rem;
  }

  .stat-card {
    padding: 1.5rem;
  }
}

@media (max-width: 576px) {
  .container-fluid {
    padding-left: 0.75rem;
    padding-right: 0.75rem;
  }

  .stat-number {
    font-size: 1.5rem;
  }
}
</style>
