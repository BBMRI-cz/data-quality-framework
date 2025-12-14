<template>
  <div class="card border-0 shadow-sm h-100 compact-card">
    <div class="card-body p-3 position-relative">
      <div class="mb-3">
        <div class="mb-2">
          <span
            class="badge border"
            :style="categoryStyle"
          >
            {{ categoryName }}
          </span>
        </div>
        <div class="d-flex align-items-center gap-2">
          <p
            class="text-muted mb-0 fw-bold flex-grow-1"
            style="font-size: 1rem; line-height: 1.3;"
            :title="qualityCheck.name || qualityCheck.cql || qualityCheck.hash"
          >
            {{ qualityCheck.name || qualityCheck.cql || qualityCheck.hash }}
          </p>
          <router-link
            :to="{ name: 'QualityCheckDetail', params: { hash: qualityCheck.hash } }"
            class="edit-icon-link"
            title="Edit quality check"
          >
            <i class="bi bi-pencil-square"></i>
          </router-link>
        </div>
        <p
          v-if="qualityCheck.description"
          class="text-muted mb-0 mt-1"
          style="font-size: 0.85rem; line-height: 1.4; opacity: 0.75;"
        >
          {{ qualityCheck.description }}
        </p>
      </div>

      <!-- No data state -->
      <div v-if="totalAgents === 0" class="text-center py-3 text-muted flex-grow-1 d-flex flex-column justify-content-center">
        <i class="bi bi-check-circle d-block mb-2 opacity-50" style="font-size: 2.5rem;"></i>
        <p class="mb-0" style="font-size: 1rem;">No results</p>
      </div>

      <!-- Coverage pie chart -->
      <div v-else class="coverage-chart flex-grow-1 d-flex flex-column align-items-center justify-content-center">
        <div class="pie-chart-shell">
          <svg
            class="pie-chart"
            viewBox="0 0 200 200"
            role="img"
            :aria-label="pieAriaLabel"
          >
            <defs>
              <linearGradient id="gradient-passed" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:#4ade80;stop-opacity:1" />
                <stop offset="100%" style="stop-color:#22c55e;stop-opacity:1" />
              </linearGradient>
              <linearGradient id="gradient-warning" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:#fcd34d;stop-opacity:1" />
                <stop offset="100%" style="stop-color:#fbbf24;stop-opacity:1" />
              </linearGradient>
              <linearGradient id="gradient-failed" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:#ff4757;stop-opacity:1" />
                <stop offset="100%" style="stop-color:#ee5a6f;stop-opacity:1" />
              </linearGradient>
            </defs>
            <g v-for="segment in pieSegments" :key="segment.key">
              <path
                v-if="segment.path && segment.percentage > 0"
                class="pie-segment"
                :d="segment.path"
                :fill="segment.gradient"
                :aria-label="getSegmentAria(segment)"
                @mouseenter="showTooltip(segment, $event)"
                @mousemove="moveTooltip($event)"
                @mouseleave="hideTooltip"
              />
            </g>
            <!-- Draw separator lines between segments -->
            <g v-for="segment in pieSegments" :key="'line-' + segment.key">
              <line
                v-if="segment.percentage > 0 && pieSegments.filter(s => s.percentage > 0).length > 1"
                :x1="100"
                :y1="100"
                :x2="segment.endX"
                :y2="segment.endY"
                stroke="white"
                stroke-width="2"
                style="pointer-events: none;"
              />
            </g>
            <!-- Draw labels on top -->
            <g v-for="segment in pieSegments" :key="'label-' + segment.key">
              <text
                v-if="segment.percentage > 5"
                :x="segment.labelX"
                :y="segment.labelY"
                text-anchor="middle"
                dominant-baseline="middle"
                class="segment-label"
                fill="white"
                font-size="14"
                font-weight="600"
                style="pointer-events: none;"
              >
                {{ segment.percentage.toFixed(0) }}%
              </text>
            </g>
          </svg>

          <!-- Custom instant tooltip -->
          <div
            v-if="tooltipVisible"
            class="custom-tooltip"
            :style="{ left: tooltipX + 'px', top: tooltipY + 'px' }"
          >
            {{ tooltipContent }}
          </div>
        </div>

        <!-- Threshold Ranges -->
        <div class="threshold-ranges mt-3">
          <div class="threshold-item">
            <div class="threshold-text">
              <span class="threshold-dot bg-success"></span>
              <span>0-{{ qualityCheck.warningThreshold }} error rate in %</span>
            </div>
            <span class="threshold-count">{{ coveragePercentages.passed.toFixed(1) }}%</span>
          </div>
          <div class="threshold-item">
            <div class="threshold-text">
              <span class="threshold-dot bg-warning"></span>
              <span>{{ qualityCheck.warningThreshold }}-{{ qualityCheck.errorThreshold }} error rate in %</span>
            </div>
            <span class="threshold-count">{{ coveragePercentages.warning.toFixed(1) }}%</span>
          </div>
          <div class="threshold-item">
            <div class="threshold-text">
              <span class="threshold-dot bg-danger"></span>
              <span>{{ qualityCheck.errorThreshold }}-100 error rate in %</span>
            </div>
            <span class="threshold-count">{{ coveragePercentages.failed.toFixed(1) }}%</span>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUpdated, ref } from 'vue'
import { CheckStatus } from '../utils/qualityCheckUtils.js'
import { Tooltip } from 'bootstrap'

// Custom tooltip state
const tooltipVisible = ref(false)
const tooltipContent = ref('')
const tooltipX = ref(0)
const tooltipY = ref(0)

const props = defineProps({
  qualityCheck: {
    type: Object,
    required: true
  },
  reports: {
    type: Array,
    required: true,
    default: () => []
  },
  agents: {
    type: Array,
    required: true,
    default: () => []
  }
})

const categoryName = computed(() => {
  return props.qualityCheck.category?.name || 'No Category'
})

const categoryStyle = computed(() => {
  const color = props.qualityCheck.category?.colorHex || '#6c757d' // Default gray
  return {
    backgroundColor: `${color}20`, // 20 is hex for ~12% opacity
    color: color,
    borderColor: `${color}40` // 40 is hex for ~25% opacity
  }
})

// Create a map of agent ID to agent name
const agentMap = computed(() => {
  const map = new Map()
  props.agents.forEach(agent => {
    map.set(agent.id, agent.name || 'Unknown Agent')
  })
  return map
})

// Aggregate results per agent for this specific check
const agentResults = computed(() => {
  const resultsMap = new Map()

  props.reports.forEach(report => {
    // Find the result for this quality check in the report
    const result = report.results?.find(r => r.hash === props.qualityCheck.hash)

    if (result) {
      const agentId = report.agentId || report.agent?.id || 'unknown'

      // Normalize result to fraction (0-1). If backend already provides percentage (0-100), convert.
      const raw = result.result
      const fraction = typeof raw === 'number' ? (raw > 1 ? raw / 100 : raw) : 0

      // Keep the worst (highest) result for each agent
      if (!resultsMap.has(agentId) || resultsMap.get(agentId).result < fraction) {
        resultsMap.set(agentId, {
          agentId,
          agentName: agentMap.value.get(agentId) || agentId,
          result: fraction,
          timestamp: report.timestamp
        })
      }
    }
  })

  return Array.from(resultsMap.values())
})

const totalAgents = computed(() => agentResults.value.length)

const coverageBuckets = computed(() => {
  return agentResults.value.reduce(
    (acc, agent) => {
      const status = getStatus(agent.result)
      if (status === CheckStatus.FAILED) {
        acc.failed += 1
      } else if (status === CheckStatus.WARNING) {
        acc.warning += 1
      } else {
        acc.passed += 1
      }
      return acc
    },
    { passed: 0, warning: 0, failed: 0 }
  )
})

const coveragePercentages = computed(() => {
  if (totalAgents.value === 0) {
    return { passed: 0, warning: 0, failed: 0 }
  }
  return {
    passed: (coverageBuckets.value.passed / totalAgents.value) * 100,
    warning: (coverageBuckets.value.warning / totalAgents.value) * 100,
    failed: (coverageBuckets.value.failed / totalAgents.value) * 100
  }
})

// Pie chart segments data
const pieSegments = computed(() => {
  if (totalAgents.value === 0) {
    return []
  }
  const segments = [
    { key: 'passed', gradient: 'url(#gradient-passed)', value: coverageBuckets.value.passed, percentage: coveragePercentages.value.passed },
    { key: 'warning', gradient: 'url(#gradient-warning)', value: coverageBuckets.value.warning, percentage: coveragePercentages.value.warning },
    { key: 'failed', gradient: 'url(#gradient-failed)', value: coverageBuckets.value.failed, percentage: coveragePercentages.value.failed }
  ]

  const radius = 90
  const toRadians = (percentage) => (percentage / 100) * Math.PI * 2 - Math.PI / 2

  // Generate path for each segment
  let cumulativePercentage = 0
  return segments.map(segment => {
    cumulativePercentage += segment.percentage
    const startAngle = toRadians(cumulativePercentage - segment.percentage)
    const endAngle = toRadians(cumulativePercentage)
    const largeArcFlag = segment.percentage > 50 ? 1 : 0

    // Calculate x, y coordinates for the arc path
    const x1 = 100 + radius * Math.cos(startAngle)
    const y1 = 100 + radius * Math.sin(startAngle)
    const x2 = 100 + radius * Math.cos(endAngle)
    const y2 = 100 + radius * Math.sin(endAngle)

    // Path data for the segment
    const path = `M 100,100 L ${x1},${y1} A ${radius},${radius} 0 ${largeArcFlag} 1 ${x2},${y2} Z`

    // Calculate label position (midpoint of the segment arc at 60% radius)
    const midAngle = (startAngle + endAngle) / 2
    const labelRadius = radius * 0.6
    const labelX = 100 + labelRadius * Math.cos(midAngle)
    const labelY = 100 + labelRadius * Math.sin(midAngle)

    return {
      ...segment,
      path,
      labelX,
      labelY,
      endX: x2,
      endY: y2
    }
  })
})

const getSegmentAria = (segment) => `${segment.key} ${segment.value} agents (${segment.percentage.toFixed(1)}%)`

// Get status based on thresholds (higher is worse)
// Accept result as fraction (0-1) or percentage (0-100)
const getStatus = (result) => {
  const percentage = result <= 1 ? result * 100 : result
  if (percentage > props.qualityCheck.errorThreshold) {
    return CheckStatus.FAILED
  } else if (percentage > props.qualityCheck.warningThreshold) {
    return CheckStatus.WARNING
  }
  return CheckStatus.PASSED
}

// Custom tooltip handlers
const showTooltip = (segment, event) => {
  // Get all agents in this segment
  const agentsInSegment = agentResults.value.filter(agent => {
    const status = getStatus(agent.result)
    return (
      (segment.key === 'passed' && status === CheckStatus.PASSED) ||
      (segment.key === 'warning' && status === CheckStatus.WARNING) ||
      (segment.key === 'failed' && status === CheckStatus.FAILED)
    )
  })

  // Display agent names
  const names = agentsInSegment.map(agent => agent.agentName).join(', ')
  tooltipContent.value = names || 'No sites'
  tooltipVisible.value = true
  moveTooltip(event)
}

const moveTooltip = (event) => {
  const rect = event.currentTarget.closest('.pie-chart-shell').getBoundingClientRect()
  tooltipX.value = event.clientX - rect.left + 10
  tooltipY.value = event.clientY - rect.top - 30
}

const hideTooltip = () => {
  tooltipVisible.value = false
}

// Initialize Bootstrap tooltips
onMounted(() => {
  initTooltips()
})

onUpdated(() => {
  initTooltips()
})

const initTooltips = () => {
  const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]')
  tooltipTriggerList.forEach(tooltipTriggerEl => {
    // Dispose existing tooltip if any
    const existingTooltip = Tooltip.getInstance(tooltipTriggerEl)
    if (existingTooltip) {
      existingTooltip.dispose()
    }
    // Create new tooltip
    new Tooltip(tooltipTriggerEl)
  })
}
</script>

<style scoped>
.compact-card {
  font-size: 0.875rem;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  will-change: transform, box-shadow;
}

.compact-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 0.5rem 1.5rem rgba(0, 0, 0, 0.15) !important;
}

.edit-icon-link {
  color: #6c757d;
  font-size: 1rem;
  text-decoration: none;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem;
  border-radius: 4px;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.edit-icon-link:hover {
  color: #0d6efd;
  background-color: rgba(13, 110, 253, 0.1);
}

.compact-card .card-body {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.coverage-chart {
  width: 100%;
  padding: 1rem 0;
  gap: 1rem;
}

.pie-chart-shell {
  position: relative;
  width: 260px;
  height: 260px;
  filter: drop-shadow(0 10px 25px rgba(15, 23, 42, 0.15));
}

.pie-chart {
  width: 100%;
  height: 100%;
}

.custom-tooltip {
  position: absolute;
  background: rgba(0, 0, 0, 0.85);
  color: white;
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  pointer-events: none;
  z-index: 1000;
  white-space: normal;
  max-width: 300px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  line-height: 1.4;
}

.pie-segment {
  cursor: default;
  transition: opacity 0.2s ease;
}

.pie-segment:hover {
  opacity: 0.85;
}

.segment-label {
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.4);
  pointer-events: none;
  user-select: none;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.threshold-ranges {
  width: 100%;
  max-width: 280px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.threshold-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  justify-content: space-between;
}

.threshold-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.threshold-text {
  color: var(--bs-body-color);
  font-size: 0.875rem;
  line-height: 1.4;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.threshold-count {
  color: var(--bs-body-color);
  font-size: 0.95rem;
  font-weight: 600;
  min-width: 2rem;
  text-align: right;
}

.bg-success {
  background: linear-gradient(135deg, #4ade80, #22c55e);
}

.bg-warning {
  background: linear-gradient(135deg, #fcd34d, #fbbf24);
}

.bg-danger {
  background: linear-gradient(135deg, #ff4757, #ee5a6f);
}
</style>
