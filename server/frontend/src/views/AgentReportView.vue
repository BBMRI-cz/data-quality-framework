<template>
  <div class="container-fluid py-3 py-md-4">
    <!-- Agent Header -->
    <div class="row mb-4">
      <div class="col-12">
        <PageHeader
          :title="agentName"
          :subtitle="`Agent ID: ${agentId}`"
          icon="bi bi-file-earmark-text-fill"
          :editable="true"
          @update:title="handleUpdateAgentName"
        />
      </div>
    </div>

    <!-- Agent Actions -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="d-flex gap-2">
          <button
            class="btn btn-outline-secondary btn-sm d-flex align-items-center"
            @click="goBack"
          >
            <i class="bi bi-arrow-left me-2"></i>
            Back to Agents
          </button>
          <button
            class="btn btn-outline-primary btn-sm d-flex align-items-center"
            @click="goToInteractions"
          >
            <i class="bi bi-clock-history me-2"></i>
            View Logs
          </button>
          <button
            class="btn btn-outline-danger btn-sm d-flex align-items-center"
            @click="confirmDeleteAgent"
            :disabled="processing"
          >
            <i class="bi bi-trash me-2"></i>
            Delete Agent
          </button>
        </div>
      </div>
    </div>

    <!-- Pending Agent Banner -->
    <div v-if="agent && agent.status === 'PENDING'" class="row mb-4">
      <div class="col-12">
        <div class="alert alert-warning d-flex align-items-center justify-content-between" role="alert">
          <div class="d-flex align-items-center">
            <i class="bi bi-exclamation-triangle me-3" style="font-size: 1.25rem;"></i>
            <div>
              <strong>Requires Attention</strong>
              <p class="mb-0 small mt-1">This agent is awaiting approval to join the network</p>
            </div>
          </div>
          <div class="d-flex gap-2 ms-3">
            <button
              class="btn btn-success btn-sm d-flex align-items-center"
              @click.stop="approveAgent(agent)"
              :disabled="processing"
            >
              <i class="bi bi-check-lg me-1"></i>
              Approve
            </button>
            <button
              class="btn btn-danger btn-sm d-flex align-items-center"
              @click.stop="declineAgent(agent)"
              :disabled="processing"
            >
              <i class="bi bi-x-lg me-1"></i>
              Decline
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading agent report...</span>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="alert alert-danger" role="alert">
      <i class="bi bi-exclamation-triangle me-2"></i>
      {{ error }}
    </div>

    <!-- Agent Report Content -->
    <div v-else>
      <!-- Stats Cards -->
      <div class="row mb-4">
        <div class="col-12 col-sm-6 col-md-3 mb-3">
          <StatsCard
            label="Total Reports"
            :value="reportStats.total"
            icon="bi bi-file-text"
            iconColor="#0d6efd"
            iconBgColor="#cfe2ff"
            trendText="All time"
            trendType="neutral"
          />
        </div>
        <div class="col-12 col-sm-6 col-md-3 mb-3">
          <StatsCard
            label="Failed Checks"
            :value="reportStats.failed"
            icon="bi bi-x-circle"
            iconColor="#dc3545"
            iconBgColor="#f8d7da"
            trendText="Needs attention"
          />
        </div>
        <div class="col-12 col-sm-6 col-md-3 mb-3">
          <StatsCard
            label="Warnings"
            :value="reportStats.warnings"
            icon="bi bi-exclamation-triangle"
            iconColor="#ffc107"
            iconBgColor="#fff3cd"
            trendText="Review recommended"
            trendType="neutral"
          />
        </div>
        <div class="col-12 col-sm-6 col-md-3 mb-3">
          <StatsCard
            label="Last Report"
            :value="reportStats.lastReportTime"
            icon="bi bi-clock"
            iconColor="#0dcaf0"
            iconBgColor="#cff4fc"
            trendText="Timestamp"
            trendType="neutral"
          />
        </div>
      </div>

      <!-- Recent Reports Table -->
      <div class="row">
        <div class="col-12">
          <ReportsTable
            :reports="reports"
            :quality-check-map="qualityCheckMap"
            @report-selected="openReportModal"
          />
        </div>
      </div>
    </div>

    <!-- Report Details Modal -->
    <ReportDetailsModal
      :report="selectedReport"
      :quality-check-map="qualityCheckMap"
      @close="closeReportModal"
    />

    <!-- Delete Confirmation Modal -->
    <BaseModal
      :show="showDeleteModal"
      @close="closeDeleteModal"
      @save="deleteAgent"
      title="Delete Agent"
      subtitle="This action cannot be undone"
      icon="bi bi-trash"
      variant="danger"
      size="sm"
      :loading="processing"
      :save-button-props="{ text: 'Delete', variant: 'danger' }"
      :cancel-button-props="{ text: 'Cancel' }"
    >
      <p class="mb-3">
        Are you sure you want to delete <strong>{{ agentName }}</strong>?
      </p>
      <p class="mb-0 text-muted small">
        This will permanently remove:
      </p>
      <ul class="text-muted small mb-0">
        <li>The agent and all its configuration</li>
        <li>All associated reports ({{ reportStats.total }} report{{ reportStats.total !== 1 ? 's' : '' }})</li>
        <li>All quality check results</li>
        <li>All interaction history</li>
      </ul>
    </BaseModal>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import StatsCard from '../components/StatsCard.vue'
import ReportsTable from '../components/ReportsTable.vue'
import ReportDetailsModal from '../components/ReportDetailsModal.vue'
import PageHeader from '../components/PageHeader.vue'
import BaseModal from '../components/BaseModal.vue'
import { apiService } from '../services/apiService.js'
import { notificationService } from '../services/notificationService.js'
import { countChecksByStatus } from '../utils/qualityCheckUtils.js'

const route = useRoute()
const router = useRouter()

const agentId = ref(route.params.uuid)
const loading = ref(true)
const error = ref(null)
const agent = ref(null)
const reports = ref([])
const qualityChecks = ref([])
const selectedReport = ref(null)
const processing = ref(false)
const showDeleteModal = ref(false)

const agentName = computed(() => {
  return agent.value?.name || 'Unknown Agent'
})

// Create a map of hash -> quality check for quick lookup
const qualityCheckMap = computed(() => {
  const map = new Map()
  qualityChecks.value.forEach(check => {
    map.set(check.hash, check)
  })
  return map
})

const reportStats = computed(() => {
  const total = reports.value.length

  // Get the latest report for stats calculation
  const sortedReports = [...reports.value].sort((a, b) =>
    new Date(b.timestamp) - new Date(a.timestamp)
  )
  const latestReport = sortedReports[0]

  if (!latestReport) {
    return {
      total: 0,
      failed: 0,
      passed: 0,
      warnings: 0,
      lastReportTime: 'N/A'
    }
  }

  const counts = countChecksByStatus(latestReport, qualityCheckMap.value)
  const lastReportTime = formatTime(latestReport.timestamp)

  return {
    total,
    failed: counts.failed,
    passed: counts.passed,
    warnings: counts.warnings,
    lastReportTime
  }
})

const fetchAgentDetails = async () => {
  try {
    loading.value = true
    error.value = null

    // Fetch quality checks first
    const qualityChecksResponse = await apiService.getQualityChecks()
    qualityChecks.value = qualityChecksResponse._embedded?.qualityChecks || []

    // Fetch agent details
    const agentsResponse = await apiService.getAgents()
    const agents = agentsResponse._embedded?.agents || []
    agent.value = agents.find(a => a.id === agentId.value)

    if (!agent.value) {
      error.value = 'Agent not found'
      return
    }

    // Fetch real reports from the API
    const reportsResponse = await apiService.getAgentReports(agentId.value)
    const reportsList = reportsResponse._embedded?.reports || reportsResponse.reports || []

    // Sort reports by timestamp (newest first)
    reports.value = reportsList.sort((a, b) =>
      new Date(b.timestamp) - new Date(a.timestamp)
    )

  } catch (err) {
    error.value = err.message || 'Failed to load agent report'
    console.error('Error fetching agent details:', err)
  } finally {
    loading.value = false
  }
}

const formatTime = (dateString) => {
  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now - date
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMins / 60)
  const diffDays = Math.floor(diffHours / 24)

  if (diffMins < 1) return 'Just now'
  if (diffMins < 60) return `${diffMins}m ago`
  if (diffHours < 24) return `${diffHours}h ago`
  if (diffDays < 7) return `${diffDays}d ago`

  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

const openReportModal = (report) => {
  selectedReport.value = report
}

const closeReportModal = () => {
  selectedReport.value = null
}

const approveAgent = async (agent) => {
  try {
    processing.value = true
    await apiService.approveAgent(agent.id)
    agent.status = 'ACTIVE'
    // Optionally, refetch agent details or update the UI accordingly
  } catch (err) {
    error.value = 'Failed to approve agent'
    console.error('Error approving agent:', err)
  } finally {
    processing.value = false
  }
}

const declineAgent = async (agent) => {
  try {
    processing.value = true
    await apiService.declineAgent(agent.id)
    agent.status = 'DECLINED'
    // Optionally, refetch agent details or update the UI accordingly
  } catch (err) {
    error.value = 'Failed to decline agent'
    console.error('Error declining agent:', err)
  } finally {
    processing.value = false
  }
}

const goToInteractions = () => {
  router.push({ name: 'AgentInteractions', params: { uuid: agentId.value } })
}

const goBack = () => {
  router.go(-1)
}

const handleUpdateAgentName = async (newName) => {
  try {
    await apiService.updateAgentName(agentId.value, newName)

    // Update the local agent object
    if (agent.value) {
      agent.value.name = newName
    }
  } catch (err) {
    error.value = 'Failed to update agent name'
    console.error('Error updating agent name:', err)
  }
}

const confirmDeleteAgent = () => {
  showDeleteModal.value = true
}

const closeDeleteModal = () => {
  showDeleteModal.value = false
}

const deleteAgent = async () => {
  try {
    processing.value = true
    const agentNameToDelete = agent.value?.name || agent.value?.id || 'Agent'
    await apiService.deleteAgent(agentId.value)

    // Show success notification
    notificationService.success(
      'Agent Deleted',
      `${agentNameToDelete} has been successfully deleted.`
    )

    // Close modal and navigate back to agents list after successful deletion
    showDeleteModal.value = false
    router.push({ name: 'Agents' })
  } catch (err) {
    error.value = 'Failed to delete agent'
    console.error('Error deleting agent:', err)
    notificationService.error(
      'Delete Failed',
      'Could not delete the agent. Please try again.'
    )
    processing.value = false
  }
}

onMounted(() => {
  fetchAgentDetails()
})
</script>

