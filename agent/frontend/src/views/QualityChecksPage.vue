<template>
  <div class="quality-checks-page">
    <PageHeader
      title="Quality Checks"
      mobile-title="Checks"
      subtitle="View and manage Data Quality Checks for your data"
      icon="bi bi-check-square"
    />

    <div class="page-content">
      <div class="page-actions">
        <ActionButton to="/quality-checks/new" icon="bi bi-plus" text="Add Quality Check" />
      </div>

      <div class="stats-grid">
        <StatCard
          :number="totalChecks"
          label="Total Checks"
          number-class="text-dark"
          help-text="Total number of quality checks configured"
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
  import ActionButton from '@/components/ActionButton.vue';
  import { useQualityChecks } from '@/composables/useQualityChecks.js';

  const { totalChecks, fetchChecks } = useQualityChecks();

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

  .page-actions {
    display: flex;
    justify-content: flex-end;
    margin-bottom: var(--spacing-md);
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
