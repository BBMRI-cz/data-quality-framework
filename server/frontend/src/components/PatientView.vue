<template>
  <div>
    <!-- Stats Cards Row -->
    <div class="stats-row mb-4">
      <StatsCard
        label="Total Patients"
        :value="`0`"
        icon="bi bi-people-fill"
        iconColor="#0d6efd"
        iconBgColor="#cfe2ff"
      />
      <StatsCard
        label="From Sites"
        :value="`0`"
        icon="bi bi-hospital-fill"
        iconColor="#198754"
        iconBgColor="#d1e7dd"
      />
      <StatsCard
        label="Total Samples"
        :value="`0`"
        icon="bi bi-eyedropper"
        iconColor="#6610f2"
        iconBgColor="#e0cffc"
      />
    </div>

    <!-- Placeholder Content -->
    <div class="placeholder-content">
      <div class="placeholder-message">
        <i class="bi bi-person-fill-gear" style="font-size: 4rem; color: #6c757d; opacity: 0.3;"></i>
        <h4 class="mt-3 text-muted">Patient-Centric View</h4>
        <p class="text-muted">Patient-level quality check data will be displayed here.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import StatsCard from './StatsCard.vue'

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
</script>

<style scoped>
/* Stats Row */
.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

/* Placeholder Content */
.placeholder-content {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 2px dashed #dee2e6;
}

.placeholder-message {
  text-align: center;
  padding: 2rem;
}

.placeholder-message h4 {
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.placeholder-message p {
  margin-bottom: 0;
  font-size: 0.95rem;
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

  .placeholder-content {
    min-height: 300px;
  }

  .placeholder-message {
    padding: 1.5rem;
  }

  .placeholder-message i {
    font-size: 3rem !important;
  }

  .placeholder-message h4 {
    font-size: 1.25rem;
  }

  .placeholder-message p {
    font-size: 0.875rem;
  }
}
</style>
