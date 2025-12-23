<template>
  <div>
    <!-- Stats Cards Row -->
    <div class="stats-row mb-4">
      <StatsCard
        label="Agents Connected"
        :value="`${agents.length}`"
        icon="bi bi-database-fill-gear"
        iconColor="#0d6efd"
        iconBgColor="#cfe2ff"
        :tooltipText="agents.map(a => a.name).join(', ')"
      />
      <StatsCard
        label="Total Quality Checks"
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
      <CategoryFilter
        :categories="categories"
        v-model="selectedCategory"
      />
    </div>

    <!-- Main Content Grid -->
    <div class="content-grid">
      <!-- Quality Checks List (one per row) -->
      <div class="quality-checks-list">
        <QualityCheckCard
          v-for="check in filteredQualityChecks"
          :key="check.hash"
          :quality-check="check"
          :reports="reports"
          :agents="agents"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import StatsCard from './StatsCard.vue'
import QualityCheckCard from './QualityCheckCard.vue'
import CategoryFilter from './CategoryFilter.vue'

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

// Total number of quality checks
const totalChecks = computed(() => {
  return props.qualityCheckMap.size
})

// Calculate sites with at least one error
const sitesWithErrors = computed(() => {
  const sitesWithErrorsSet = new Set()

  props.reports.forEach(report => {
    report.results?.forEach(result => {
      const qualityCheck = props.qualityCheckMap.get(result.hash)
      if (qualityCheck) {
        const raw = result.result
        const fraction = typeof raw === 'number' ? (raw > 1 ? raw / 100 : raw) : 0
        const percentage = fraction * 100

        if (percentage > qualityCheck.errorThreshold) {
          const agentId = report.agentId || report.agent?.id
          if (agentId) {
            sitesWithErrorsSet.add(agentId)
          }
        }
      }
    })
  })

  return sitesWithErrorsSet.size
})

// Calculate sites with at least one warning
const sitesWithWarnings = computed(() => {
  const sitesWithWarningsSet = new Set()

  props.reports.forEach(report => {
    report.results?.forEach(result => {
      const qualityCheck = props.qualityCheckMap.get(result.hash)
      if (qualityCheck) {
        const raw = result.result
        const fraction = typeof raw === 'number' ? (raw > 1 ? raw / 100 : raw) : 0
        const percentage = fraction * 100

        // Check if it's in warning range (above warning threshold but not error)
        if (percentage > qualityCheck.warningThreshold && percentage <= qualityCheck.errorThreshold) {
          const agentId = report.agentId || report.agent?.id
          if (agentId) {
            sitesWithWarningsSet.add(agentId)
          }
        }
      }
    })
  })

  return sitesWithWarningsSet.size
})

// Get array of quality checks for iteration
const qualityChecks = computed(() => {
  return Array.from(props.qualityCheckMap.values())
})

const filteredQualityChecks = computed(() => {
  if (!selectedCategory.value) {
    return qualityChecks.value
  }
  return qualityChecks.value.filter(check => {
    const categoryName = check.category?.name || 'No Category'
    return categoryName === selectedCategory.value
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

/* Main Content Grid */
.content-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: 1fr;
}

.quality-checks-list {
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

  .quality-checks-list {
    gap: 0;
  }
}
</style>
