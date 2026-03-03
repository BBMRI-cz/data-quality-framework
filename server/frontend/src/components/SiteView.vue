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
        icon="bi bi-check-square-fill"
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
import { toRefs } from 'vue'
import StatsCard from './StatsCard.vue'
import AgentCard from './AgentCard.vue'
import CategoryFilter from './CategoryFilter.vue'
import { useSiteStats } from '../composables/useSiteStats'

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

const { reports, qualityCheckMap, agents } = toRefs(props)

const {
  selectedCategory,
  selectedGroup,
  categories,
  groups,
  filteredAgents,
  totalChecks,
  sitesWithErrors,
  sitesWithWarnings
} = useSiteStats(reports, qualityCheckMap, agents)
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
