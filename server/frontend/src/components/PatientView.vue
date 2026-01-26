<template>
  <div>
    <!-- Stats Cards Row -->
    <div class="stats-row mb-4">
      <StatsCard
        label="Total Patients"
        :value="`${totalPatients.toLocaleString()}`"
        icon="bi bi-people-fill"
        iconColor="#0d6efd"
        iconBgColor="#cfe2ff"
      />
      <StatsCard
        label="From Sites"
        :value="`${fromSites}`"
        icon="bi bi-hospital-fill"
        iconColor="#198754"
        iconBgColor="#d1e7dd"
      />
      <StatsCard
        label="Total Samples"
        :value="`${totalSamples.toLocaleString()}`"
        icon="bi bi-eyedropper"
        iconColor="#6610f2"
        iconBgColor="#e0cffc"
      />
    </div>

    <!-- Category Filter -->
    <div class="mb-4">
      <div class="filter-label">Categories:</div>
      <CategoryFilter
        :categories="categories"
        v-model="selectedCategory"
      />
    </div>

    <!-- Group Filter -->
    <div class="mb-4" v-if="groups.length > 0">
      <div class="filter-label">Groups:</div>
      <CategoryFilter
        :categories="groups"
        v-model="selectedGroup"
      />
    </div>

    <!-- Main Content Grid -->
    <div class="content-grid">
      <!-- No Results Message -->
      <div v-if="aggregatedCheckResults.length === 0" class="no-results">
        <i class="bi bi-info-circle"></i>
        <span>No quality checks match the selected criteria</span>
      </div>

      <!-- Quality Checks List (one per row) -->
      <div v-else class="checks-list">
        <QualityCheckRow
          v-for="check in aggregatedCheckResults"
          :key="check.checkHash"
          :check-hash="check.checkHash"
          :check-name="check.checkName"
          :category="check.category"
          :patients-meeting-criteria="check.patientsMeetingCriteria"
          :total-patients="totalPatients"
          :quality-check="check.qualityCheck"
          :reports="reports"
          :agents="agents"
          :selected-category="selectedCategory"
          :selected-group="selectedGroup"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import StatsCard from './StatsCard.vue'
import CategoryFilter from './CategoryFilter.vue'
import QualityCheckRow from './QualityCheckRow.vue'

const props = defineProps({
  reports: {
    type: Array,
    required: true
  },
  qualityCheckMap: {
    type: Map,
    required: true
  },
  agents: {
    type: Array,
    required: true
  }
})

const selectedCategory = ref(null)
const selectedGroup = ref(null)

/**
 * Get unique categories from quality checks
 */
const categories = computed(() => {
  const cats = new Set()
  props.qualityCheckMap.forEach(check => {
    if (check.category && check.category.name) {
      cats.add(check.category.name)
    } else {
      cats.add('No Category')
    }
  })
  return Array.from(cats).sort()
})

/**
 * Get unique groups from agents
 */
const groups = computed(() => {
  const groupSet = new Set()
  props.agents.forEach(agent => {
    if (agent.groups && Array.isArray(agent.groups)) {
      agent.groups.forEach(group => {
        if (group && group.name) {
          groupSet.add(group.name)
        }
      })
    }
  })
  return Array.from(groupSet).sort()
})

/**
 * Get the most recent report per agent that has totalPatients and totalSamples data
 */
const mostRecentReportsWithData = computed(() => {
  const reportsByAgent = new Map()

  props.reports.forEach(report => {
    // Only include reports that have totalPatients and totalSamples defined (not null/undefined/0)
    if (report.totalPatients != null && report.totalSamples != null) {
      const agentId = report.agentId
      const existing = reportsByAgent.get(agentId)

      if (!existing || new Date(report.timestamp) > new Date(existing.timestamp)) {
        reportsByAgent.set(agentId, report)
      }
    }
  })

  return Array.from(reportsByAgent.values())
})

/**
 * Filter reports by selected group and category
 */
const filteredReports = computed(() => {
  let filtered = mostRecentReportsWithData.value

  // Filter by group if selected
  if (selectedGroup.value) {
    filtered = filtered.filter(report => {
      const agent = props.agents.find(a => a.id === report.agentId)
      if (!agent || !agent.groups || !Array.isArray(agent.groups)) {
        return false
      }
      return agent.groups.some(group => group && group.name === selectedGroup.value)
    })
  }

  // Filter by category if selected - only include reports that have at least one check in the selected category
  if (selectedCategory.value) {
    filtered = filtered.filter(report => {
      if (!report.results) return false

      return report.results.some(result => {
        const qualityCheck = props.qualityCheckMap.get(result.hash)
        if (!qualityCheck) return false

        const categoryName = qualityCheck.category?.name || 'No Category'
        return categoryName === selectedCategory.value
      })
    })
  }

  return filtered
})

/**
 * Total number of patients across all sites (filtered by group)
 */
const totalPatients = computed(() => {
  return filteredReports.value.reduce((sum, report) => {
    return sum + (report.totalPatients || 0)
  }, 0)
})

/**
 * Total number of samples across all sites (filtered by group)
 */
const totalSamples = computed(() => {
  return filteredReports.value.reduce((sum, report) => {
    return sum + (report.totalSamples || 0)
  }, 0)
})

/**
 * Number of sites (agents) that have submitted reports with patient data (filtered by group)
 */
const fromSites = computed(() => {
  return filteredReports.value.length
})

/**
 * Aggregate quality check results across all sites
 * For each quality check, calculate the total number of patients meeting the criteria
 * Result is (0..1) representing percentage, so we multiply by totalPatients from that site
 */
const aggregatedCheckResults = computed(() => {
  const checkMap = new Map()

  // Process filtered reports (already filtered by group)
  filteredReports.value.forEach(report => {
    if (!report.results || !report.totalPatients) return

    report.results.forEach(result => {
      const qualityCheck = props.qualityCheckMap.get(result.hash)
      if (!qualityCheck) return

      // Filter by category if selected
      if (selectedCategory.value) {
        const categoryName = qualityCheck.category?.name || 'No Category'
        if (categoryName !== selectedCategory.value) return
      }

      // Get or create aggregated entry for this check
      if (!checkMap.has(result.hash)) {
        checkMap.set(result.hash, {
          checkHash: result.hash,
          checkName: qualityCheck.name || qualityCheck.cql || result.hash,
          category: qualityCheck.category?.name || null,
          qualityCheck: qualityCheck,
          patientsMeetingCriteria: 0
        })
      }

      const entry = checkMap.get(result.hash)

      // Result is a percentage (0..1), multiply by the site's total patients
      // to get the number of patients meeting the criteria from this site
      const resultPercentage = result.result <= 1 ? result.result : result.result / 100
      const patientsFromThisSite = Math.round(resultPercentage * report.totalPatients)

      entry.patientsMeetingCriteria += patientsFromThisSite
    })
  })

  // Convert to array and sort by status (failed first, then warning, then passed)
  return Array.from(checkMap.values()).sort((a, b) => {
    // Calculate status for each
    const getStatus = (check) => {
      if (totalPatients.value === 0) return 2 // PASSED if no data
      const percentage = (check.patientsMeetingCriteria / totalPatients.value) * 100
      if (percentage > check.qualityCheck.errorThreshold) return 0 // FAILED
      if (percentage > check.qualityCheck.warningThreshold) return 1 // WARNING
      return 2 // PASSED
    }

    const statusA = getStatus(a)
    const statusB = getStatus(b)

    if (statusA !== statusB) {
      return statusA - statusB
    }

    // Same status, sort by name
    return a.checkName.localeCompare(b.checkName)
  })
})
</script>

<style scoped>
/* Stats Row */
.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
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

/* Main Content Grid */
.content-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: 1fr;
}

/* No Results State */
.no-results {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-xl);
  color: var(--color-gray-500);
  font-style: italic;
  font-size: 0.9rem;
  background-color: var(--color-gray-50);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-gray-200);
}

.no-results i {
  font-size: 1.2rem;
}

.checks-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* Tablet Layout */
@media (min-width: 768px) and (max-width: 991px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* Mobile Layout */
@media (max-width: 767px) {
  .stats-row {
    grid-template-columns: 1fr;
    gap: 0.75rem;
  }

  .content-grid {
    gap: 0.75rem;
  }

  .checks-list {
    gap: 0;
  }

  .no-results {
    padding: var(--spacing-lg);
    font-size: 0.85rem;
  }
}
</style>
