<template>
  <div>
    <!-- Stats Cards Row -->
    <div class="stats-row mb-4">
      <StatsCard
        label="Agents"
        :value="`${filteredAgents.length}`"
        icon="bi bi-database-fill-gear"
        iconColor="#0d6efd"
        iconBgColor="#cfe2ff"
        :tooltipText="filteredAgents.map(a => a.name).join(', ')"
      />
      <StatsCard
        label="Quality Checks"
        :value="`${totalChecks}`"
        icon="bi bi-clipboard-check-fill"
        iconColor="#6f42c1"
        iconBgColor="#e2d9f3"
      />
      <StatsCard
        label="Agents with Errors"
        :value="`${sitesWithErrors}`"
        icon="bi bi-exclamation-triangle-fill"
        iconColor="#dc3545"
        iconBgColor="#f8d7da"
      />
      <StatsCard
        label="Agents with Warnings"
        :value="`${sitesWithWarnings}`"
        icon="bi bi-exclamation-circle-fill"
        iconColor="#ffc107"
        iconBgColor="#fff3cd"
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
      <!-- Agents List (one per row) -->
      <div class="agents-list">
        <AgentCard
          v-for="agent in filteredAgents"
          :key="agent.id"
          :agent="agent"
          :reports="reports"
          :quality-check-map="qualityCheckMap"
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
import AgentCard from './AgentCard.vue'
import CategoryFilter from './CategoryFilter.vue'
import { CheckStatus } from '../utils/qualityCheckUtils.js'

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

// Helper function - exact same logic as AgentCard.getResultStatus
function getResultStatus(result, qualityCheck) {
  const percentage = result <= 1 ? result * 100 : result

  if (percentage > qualityCheck.errorThreshold) {
    return CheckStatus.FAILED
  } else if (percentage > qualityCheck.warningThreshold) {
    return CheckStatus.WARNING
  }
  return CheckStatus.PASSED
}

// Get unique categories from quality checks
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

// Get unique groups from agents
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

// Filter agents based on selected group
const filteredAgents = computed(() => {
  if (!selectedGroup.value) {
    return props.agents
  }

  return props.agents.filter(agent => {
    if (!agent.groups || !Array.isArray(agent.groups)) {
      return false
    }
    return agent.groups.some(group => group && group.name === selectedGroup.value)
  })
})

// Total number of quality checks (filtered by category if selected)
const totalChecks = computed(() => {
  if (!selectedCategory.value) {
    return props.qualityCheckMap.size
  }

  let count = 0
  props.qualityCheckMap.forEach(check => {
    const categoryName = check.category && check.category.name ? check.category.name : 'No Category'
    if (categoryName === selectedCategory.value) {
      count++
    }
  })
  return count
})

// Calculate sites with at least one error (filtered by group and category)
// Note: An agent is counted here ONLY if it has at least one check that EXCEEDS the error threshold
const sitesWithErrors = computed(() => {
  const sitesWithErrorsSet = new Set()
  const filteredAgentIds = new Set(filteredAgents.value.map(agent => agent.id))

  // Group reports by agent and get the latest report for each
  const latestReportsByAgent = new Map()
  props.reports.forEach(report => {
    const reportAgentId = report.agentId || report.agent?.id
    if (!reportAgentId || !filteredAgentIds.has(reportAgentId)) return

    const existing = latestReportsByAgent.get(reportAgentId)
    if (!existing || new Date(report.timestamp) > new Date(existing.timestamp)) {
      latestReportsByAgent.set(reportAgentId, report)
    }
  })

  // Check only the latest report for each agent
  latestReportsByAgent.forEach((report, agentId) => {
    report.results?.forEach(result => {
      const qualityCheck = props.qualityCheckMap.get(result.hash)
      if (!qualityCheck) return

      // Filter by category if one is selected
      if (selectedCategory.value) {
        const categoryName = qualityCheck.category && qualityCheck.category.name
          ? qualityCheck.category.name
          : 'No Category'
        if (categoryName !== selectedCategory.value) return
      }

      const raw = result.result
      if (typeof raw !== 'number' || isNaN(raw)) return

      // Use the same logic as AgentCard
      const status = getResultStatus(raw, qualityCheck)
      if (status === CheckStatus.FAILED) {
        sitesWithErrorsSet.add(agentId)
      }
    })
  })

  return sitesWithErrorsSet.size
})

// Calculate sites with at least one warning (filtered by group and category)
// Note: An agent is counted here ONLY if it has at least one check in WARNING range
// Warning range: percentage > warningThreshold AND percentage <= errorThreshold
const sitesWithWarnings = computed(() => {
  const sitesWithWarningsSet = new Set()
  const filteredAgentIds = new Set(filteredAgents.value.map(agent => agent.id))

  // Group reports by agent and get the latest report for each
  const latestReportsByAgent = new Map()
  props.reports.forEach(report => {
    const reportAgentId = report.agentId || report.agent?.id
    if (!reportAgentId || !filteredAgentIds.has(reportAgentId)) return

    const existing = latestReportsByAgent.get(reportAgentId)
    if (!existing || new Date(report.timestamp) > new Date(existing.timestamp)) {
      latestReportsByAgent.set(reportAgentId, report)
    }
  })

  // Check only the latest report for each agent
  latestReportsByAgent.forEach((report, agentId) => {
    report.results?.forEach(result => {
      const qualityCheck = props.qualityCheckMap.get(result.hash)
      if (!qualityCheck) return

      // Filter by category if one is selected
      if (selectedCategory.value) {
        const categoryName = qualityCheck.category && qualityCheck.category.name
          ? qualityCheck.category.name
          : 'No Category'
        if (categoryName !== selectedCategory.value) return
      }

      const raw = result.result
      if (typeof raw !== 'number' || isNaN(raw)) return

      // Use the same logic as AgentCard
      const status = getResultStatus(raw, qualityCheck)
      if (status === CheckStatus.WARNING) {
        sitesWithWarningsSet.add(agentId)
      }
    })
  })


  return sitesWithWarningsSet.size
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

.agents-list {
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

  .agents-list {
    gap: 0;
  }
}
</style>
