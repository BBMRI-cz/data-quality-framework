<template>
  <div class="container-fluid py-3 py-md-4">
    <!-- Dashboard Header -->
    <PageHeader
      :title="headerTitle"
      :subtitle="headerSubtitle"
      :icon="headerIcon"
      :hide-subtitle-on-mobile="false"
    >
      <template #toggle>
        <!-- View Toggle -->
        <div class="view-toggle">
          <button
            :class="['toggle-option', { active: viewMode === 'site' }]"
            @click="viewMode = 'site'"
            title="Site-centric view"
          >
            <i class="bi bi-database-fill-gear"></i>
            <span class="toggle-label">Sites</span>
          </button>
          <button
            :class="['toggle-option', { active: viewMode === 'patient' }]"
            @click="viewMode = 'patient'"
            title="Patient-centric view"
          >
            <i class="bi bi-person"></i>
            <span class="toggle-label">Patients</span>
          </button>
        </div>
      </template>
    </PageHeader>

    <!-- Loading state -->
    <div v-if="loading" class="loading-state">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <!-- View Components with Transition -->
    <Transition v-else name="view-fade" mode="out-in">
      <SiteView
        v-if="viewMode === 'site'"
        key="site-view"
        :reports="reports"
        :quality-check-map="qualityCheckMap"
        :agents="agents"
      />
      <PatientView
        v-else-if="viewMode === 'patient'"
        key="patient-view"
        :reports="reports"
        :quality-check-map="qualityCheckMap"
        :agents="agents"
      />
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '../components/PageHeader.vue'
import SiteView from '../components/SiteView.vue'
import PatientView from '../components/PatientView.vue'
import { apiService } from '../services/apiService.js'

const route = useRoute()
const router = useRouter()

const reports = ref([])
const qualityCheckMap = ref(new Map())
const agents = ref([])

// Initialize viewMode from URL query parameter, default to 'site'
const viewMode = ref(route.query.view === 'patient' ? 'patient' : 'site')

// Watch for viewMode changes and update URL
watch(viewMode, (newView) => {
  router.push({
    query: { ...route.query, view: newView }
  })
})

// Watch for URL changes (browser back/forward) and update viewMode
watch(() => route.query.view, (newView) => {
  if (newView && (newView === 'site' || newView === 'patient')) {
    viewMode.value = newView
  }
})
const viewMode = ref('site') // 'site' or 'patient'
const loading = ref(true)

const headerTitle = computed(() =>
  viewMode.value === 'site' ? 'Site Performance Overview' : 'Patient Data Overview'
)

const headerSubtitle = computed(() =>
  viewMode.value === 'site'
    ? 'Review Data Quality metrics from all connected locations'
    : 'Review Data Quality metrics across patient records'
)

const headerIcon = computed(() =>
  viewMode.value === 'site' ? 'bi bi-database-fill-gear' : 'bi bi-person-fill'
)

const loadReportsData = async () => {
  try {
    loading.value = true

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
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadReportsData()
})
</script>

<style scoped>
/* Loading State */
.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

/* View Toggle */

.view-toggle {
  display: inline-flex;
  background: #f8f9fa;
  border-radius: 12px;
  padding: 4px;
  gap: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}


.toggle-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1.25rem;
  border: none;
  background: transparent;
  color: #6c757d;
  font-weight: 500;
  font-size: 0.95rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}


.toggle-option i {
  font-size: 1.1rem;
}

.toggle-option:hover {
  color: #495057;
  background: rgba(102, 126, 234, 0.1);
}

.toggle-option.active {
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.toggle-label {
  font-size: 0.9rem;
}

/* View Transition Animations */
.view-fade-enter-active,
.view-fade-leave-active {
  transition: all 0.3s ease;
}

.view-fade-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.view-fade-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

.view-fade-enter-to,
.view-fade-leave-from {
  opacity: 1;
  transform: translateY(0);
}

/* Mobile Layout */
@media (max-width: 767px) {
  .container-fluid {
    padding-left: 0.75rem;
    padding-right: 0.75rem;
  }

  .view-toggle {
    width: 100%;
    max-width: 350px;
  }

  .toggle-option {
    flex: 1;
    justify-content: center;
    padding: 0.5rem 0.75rem;
  }

  .toggle-label {
    font-size: 0.85rem;
  }
}
</style>
