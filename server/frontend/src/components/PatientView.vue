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
import { toRefs } from 'vue'
import StatsCard from './StatsCard.vue'
import CategoryFilter from './CategoryFilter.vue'
import QualityCheckRow from './QualityCheckRow.vue'
import { usePatientStats } from '../composables/usePatientStats'

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
  totalPatients,
  totalSamples,
  fromSites,
  aggregatedCheckResults
} = usePatientStats(reports, qualityCheckMap, agents)
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
