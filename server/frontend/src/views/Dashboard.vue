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
        <div class="d-flex flex-column align-items-end gap-2">
          <!-- View Toggle -->
          <div class="view-toggle">
            <button
              :class="['toggle-option', { active: viewMode === 'site' }]"
              @click="viewMode = 'site'"
              title="Site-centric view"
            >
              <i class="bi bi-hospital"></i>
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

          <!-- Show Numbers Toggle -->
          <div class="view-toggle view-toggle-sm">
            <button
              :class="['toggle-option', { active: !showNumbers }]"
              @click="showNumbers = false"
              title="Show percentages"
            >
              <span class="toggle-label">%</span>
            </button>
            <button
              :class="['toggle-option', { active: showNumbers }]"
              @click="showNumbers = true"
              title="Show counts"
            >
              <span class="toggle-label">#</span>
            </button>
          </div>
        </div>
      </template>
    </PageHeader>

    <!-- View Components with Transition -->
    <Transition name="view-fade" mode="out-in">
      <SiteView
        v-if="viewMode === 'site'"
        key="site-view"
        :reports="reports"
        :quality-check-map="qualityCheckMap"
        :agents="agents"
        :show-numbers="showNumbers"
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
import { ref, onMounted, computed } from 'vue'
import PageHeader from '../components/PageHeader.vue'
import SiteView from '../components/SiteView.vue'
import PatientView from '../components/PatientView.vue'
import { apiService } from '../services/apiService.js'

const reports = ref([])
const qualityCheckMap = ref(new Map())
const agents = ref([])
const viewMode = ref('site') // 'site' or 'patient'
const showNumbers = ref(false)

const headerTitle = computed(() =>
  viewMode.value === 'site' ? 'Site Performance Overview' : 'Patient Data Overview'
)

const headerSubtitle = computed(() =>
  viewMode.value === 'site'
    ? 'Review Data Quality metrics from all connected locations'
    : 'Review Data Quality metrics across patient records'
)

const headerIcon = computed(() =>
  viewMode.value === 'site' ? 'bi bi-hospital-fill' : 'bi bi-person-fill'
)

const loadReportsData = async () => {
  try {
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
  }
}

onMounted(() => {
  loadReportsData()
})
</script>

<style scoped>
/* View Toggle */

.view-toggle {
  display: inline-flex;
  background: #f8f9fa;
  border-radius: 12px;
  padding: 4px;
  gap: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.view-toggle-sm {
  border-radius: 8px;
  padding: 3px;
  gap: 2px;
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

.view-toggle-sm .toggle-option {
  padding: 0.25rem 0.75rem;
  font-size: 0.85rem;
  border-radius: 6px;
  min-width: 40px;
  justify-content: center;
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
