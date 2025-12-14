<template>
  <div>
    <!-- Stats Cards Row -->
    <div class="stats-row mb-4">
      <StatsCard
        label="Sites Monitored"
        :value="`${agents.length}`"
        icon="bi bi-geo-alt-fill"
        iconColor="#0d6efd"
        iconBgColor="#cfe2ff"
        :tooltipText="agents.map(a => a.name).join(', ')"
      />
      <StatsCard
        label="Total Reports"
        :value="`${reports.length}`"
        icon="bi bi-file-earmark-text-fill"
        iconColor="#6f42c1"
        iconBgColor="#e2d9f3"
      />
      <StatsCard
        label="Errors This Week"
        :value="`${errorsThisWeek}`"
        icon="bi bi-exclamation-triangle-fill"
        iconColor="#dc3545"
        iconBgColor="#f8d7da"
        :trendText="errorsChange"
        :trendType="errorsTrendType"
      />
      <StatsCard
        label="Warnings This Week"
        :value="`${warningsThisWeek}`"
        icon="bi bi-exclamation-circle-fill"
        iconColor="#ffc107"
        iconBgColor="#fff3cd"
        :trendText="warningsChange"
        :trendType="warningsTrendType"
      />
    </div>

    <!-- Category Filter -->
    <div class="mb-4 d-flex gap-2 flex-wrap">
      <button
        class="btn btn-sm rounded-pill"
        :class="selectedCategory === null ? 'btn-custom-primary' : 'btn-outline-custom-primary'"
        @click="selectedCategory = null"
      >
        All
      </button>
      <button
        v-for="category in categories"
        :key="category"
        class="btn btn-sm rounded-pill"
        :class="selectedCategory === category ? 'btn-custom-primary' : 'btn-outline-custom-primary'"
        @click="selectedCategory = category"
      >
        {{ category }}
      </button>
    </div>

    <!-- Main Content Grid -->
    <div class="content-grid">
      <!-- Quality Checks Grid -->
      <div class="quality-checks-grid">
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
import { getReportStatus, CheckStatus } from '../utils/qualityCheckUtils.js'

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

// Get reports from this week (last 7 days)
const reportsThisWeek = computed(() => {
  const oneWeekAgo = new Date()
  oneWeekAgo.setDate(oneWeekAgo.getDate() - 7)

  return props.reports.filter(report => {
    const reportDate = new Date(report.timestamp)
    return reportDate >= oneWeekAgo
  })
})

// Get reports from the previous week (8-14 days ago)
const reportsLastWeek = computed(() => {
  const twoWeeksAgo = new Date()
  twoWeeksAgo.setDate(twoWeeksAgo.getDate() - 14)
  const oneWeekAgo = new Date()
  oneWeekAgo.setDate(oneWeekAgo.getDate() - 7)

  return props.reports.filter(report => {
    const reportDate = new Date(report.timestamp)
    return reportDate >= twoWeeksAgo && reportDate < oneWeekAgo
  })
})

// Count errors from this week
const errorsThisWeek = computed(() => {
  return reportsThisWeek.value.filter(report => {
    const status = getReportStatus(report, props.qualityCheckMap)
    return status === CheckStatus.FAILED
  }).length
})

// Count errors from last week
const errorsLastWeek = computed(() => {
  return reportsLastWeek.value.filter(report => {
    const status = getReportStatus(report, props.qualityCheckMap)
    return status === CheckStatus.FAILED
  }).length
})

// Count warnings from this week
const warningsThisWeek = computed(() => {
  return reportsThisWeek.value.filter(report => {
    const status = getReportStatus(report, props.qualityCheckMap)
    return status === CheckStatus.WARNING
  }).length
})

// Count warnings from last week
const warningsLastWeek = computed(() => {
  return reportsLastWeek.value.filter(report => {
    const status = getReportStatus(report, props.qualityCheckMap)
    return status === CheckStatus.WARNING
  }).length
})

// Calculate change in errors from last week
const errorsChange = computed(() => {
  const change = errorsThisWeek.value - errorsLastWeek.value
  if (change === 0) return 'No change from last week'
  const direction = change > 0 ? '+' : ''
  return `${direction}${change} from last week`
})

// Calculate change in warnings from last week
const warningsChange = computed(() => {
  const change = warningsThisWeek.value - warningsLastWeek.value
  if (change === 0) return 'No change from last week'
  const direction = change > 0 ? '+' : ''
  return `${direction}${change} from last week`
})

// Determine trend type for errors (fewer is better)
const errorsTrendType = computed(() => {
  const change = errorsThisWeek.value - errorsLastWeek.value
  if (change < 0) return 'positive'  // fewer errors is positive
  if (change > 0) return 'negative'  // more errors is negative
  return 'neutral'
})

// Determine trend type for warnings (fewer is better)
const warningsTrendType = computed(() => {
  const change = warningsThisWeek.value - warningsLastWeek.value
  if (change < 0) return 'positive'  // fewer warnings is positive
  if (change > 0) return 'negative'  // more warnings is negative
  return 'neutral'
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
/* Custom Primary Button */
.btn-custom-primary {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

.btn-custom-primary:hover {
  background-color: var(--color-primary-dark);
  border-color: var(--color-primary-dark);
  color: #fff;
}

.btn-outline-custom-primary {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background-color: transparent;
}

.btn-outline-custom-primary:hover {
  background-color: var(--color-primary);
  color: #fff;
}

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

.quality-checks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1rem;
  align-items: start;
}

.quality-checks-grid > * {
  min-height: 150px;
}

/* Desktop Layout */
@media (min-width: 992px) {
  .quality-checks-grid {
    grid-template-columns: repeat(3, 1fr);
    max-width: 100%;
  }

  .quality-checks-grid > * {
    height: 500px;
  }
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

  .quality-checks-grid {
    grid-template-columns: 1fr;
    gap: 0.75rem;
  }

  .quality-checks-grid > * {
    min-height: auto;
  }
}
</style>
