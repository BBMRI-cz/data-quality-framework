<template>
  <div class="quality-checks-page">
    <PageHeader
      title="Quality Checks"
      mobile-title="Checks"
      subtitle="View and manage quality checks for your data"
      icon="bi bi-check2-square"
    />

    <div class="page-content">
      <div class="stats-grid">
        <StatCard
          :number="totalChecks"
          label="Total Checks"
          number-class="text-dark"
          help-text="Total number of quality checks configured"
        />
        <StatCard
          :number="totalEpsilonBudget.toFixed(2)"
          label="Total Epsilon Budget"
          number-class="text-primary"
          help-text="Combined epsilon budget across all quality checks for differential privacy"
        />
      </div>

      <QualityCheckTable />
    </div>
  </div>
</template>

<script setup>
  import { onMounted } from 'vue';
  import QualityCheckTable from '@/components/QualityCheckTable.vue';
  import PageHeader from '@/components/PageHeader.vue';
  import StatCard from '@/components/StatCard.vue';
  import { useQualityChecks } from '@/composables/useQualityChecks.js';

  const { totalChecks, totalEpsilonBudget, fetchChecks } = useQualityChecks();

  onMounted(fetchChecks);
</script>

<style scoped>
  .quality-checks-page {
    min-height: 100%;
    padding: var(--spacing-xl);
  }

  .page-content {
    width: 100%;
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: var(--spacing-md);
    margin-bottom: var(--spacing-lg);
  }

  @media (max-width: 768px) {
    .quality-checks-page {
      padding: var(--spacing-md);
    }
  }

  @media (max-width: 576px) {
    .quality-checks-page {
      padding: var(--spacing-sm);
    }

    .stats-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
