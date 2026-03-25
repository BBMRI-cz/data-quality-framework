<template>
  <div>
    <!-- Stats Cards Row -->
    <div class="stats-row mb-4">
      <StatsCard
        label="Agents"
        :value="`${filteredAgents.length}`"
        icon="bi bi-database-fill-gear"
        color="var(--color-primary)"
        :tooltip-text="filteredAgents.map((a) => a.name).join(', ')"
      />
      <StatsCard
        label="Quality Checks"
        :value="`${totalChecks}`"
        icon="bi bi-check-square-fill"
        color="var(--color-primary)"
      />
      <StatsCard
        label="Agents with Errors"
        :value="`${sitesWithErrors}`"
        :icon="failedStatus.icon"
        :color="failedStatus.color"
      />
      <StatsCard
        label="Agents with Warnings"
        :value="`${sitesWithWarnings}`"
        :icon="warningStatus.icon"
        :color="warningStatus.color"
      />
    </div>

    <!-- Category Filter -->
    <div class="mb-4">
      <LabeledValuesFilter
        v-model="selectedCategory"
        label="Categories:"
        :categories="categories"
      />
    </div>

    <!-- Group Filter -->
    <div v-if="groups.length > 0" class="mb-4">
      <LabeledValuesFilter v-model="selectedGroup" label="Groups:" :categories="groups" />
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
  import { toRefs } from 'vue';
  import { CheckStatus } from '@/utils/qualityCheckUtils.js';
  import { useStatuses } from '@/composables/useStatuses.js';
  import StatsCard from '../ui/StatsCard.vue';
  import AgentCard from '../agent/AgentCard.vue';
  import LabeledValuesFilter from '@/components/ui/LabeledValuesFilter.vue';
  import { useSiteStats } from '@/composables/useSiteStats.js';

  const { getStatusMeta } = useStatuses();
  const failedStatus = getStatusMeta(CheckStatus.FAILED);
  const warningStatus = getStatusMeta(CheckStatus.WARNING);

  const props = defineProps({
    reports: {
      type: Array,
      required: true,
    },
    qualityCheckMap: {
      type: Map,
      required: true,
    },
    agents: {
      type: Array,
      required: true,
    },
  });

  const { reports, qualityCheckMap, agents } = toRefs(props);

  const {
    selectedCategory,
    selectedGroup,
    categories,
    groups,
    filteredAgents,
    totalChecks,
    sitesWithErrors,
    sitesWithWarnings,
  } = useSiteStats(reports, qualityCheckMap, agents);
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
