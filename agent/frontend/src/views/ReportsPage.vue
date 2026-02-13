<template>
  <div class="reports-page">
    <PageHeader
      title="Reports"
      mobile-title="Reports"
      subtitle="View and analyze all data quality reports"
      icon="bi bi-file-earmark-text"
    />

    <div class="page-content">
      <!-- Action Section -->
      <div class="d-flex justify-content-between align-items-center mb-3 mb-md-4">
        <div></div>
        <GenerateReportButton
          :loading="reportStore.isGenerating"
          text="Generate Report"
          @click="generateReport"
        />
      </div>

      <!-- Stats Cards -->
      <div class="stats-grid mb-3 mb-md-4">
        <StatCard
          :number="reportStore.reports.length"
          label="Total Reports"
          number-class="text-dark"
        />
        <StatCard :number="latestReportTime" label="Latest Report" number-class="text-primary" />
      </div>

      <!-- Loading state -->
      <div
        v-if="reportStore.isGenerating && reportStore.reports.length === 0"
        class="loading-state"
      >
        <div class="spinner-border text-primary" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
      </div>

      <!-- Reports table -->
      <ReportsTable v-else :reports="sortedReports" />
    </div>
  </div>
</template>

<script setup>
  import { computed, onMounted } from 'vue';
  import PageHeader from '@/components/PageHeader.vue';
  import ReportsTable from '@/components/ReportsTable.vue';
  import StatCard from '@/components/StatCard.vue';
  import GenerateReportButton from '@/components/GenerateReportButton.vue';
  import { useReportStore } from '@/stores/reportStore.js';

  const reportStore = useReportStore();

  const sortedReports = computed(() => {
    return [...reportStore.reports].sort(
      (a, b) => new Date(b.generatedAt) - new Date(a.generatedAt)
    );
  });

  const latestReportTime = computed(() => {
    if (reportStore.reports.length === 0) {
      return 'No reports yet';
    }
    const latest = sortedReports.value[0];
    return formatDateShort(latest.generatedAt);
  });

  const formatDateShort = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  const generateReport = async () => {
    await reportStore.generateReport();
  };

  onMounted(() => {
    reportStore.fetchReports();
  });
</script>

<style scoped>
  .reports-page {
    min-height: 100%;
    padding: 2rem;
  }

  .page-content {
    width: 100%;
  }

  .loading-state {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 300px;
  }

  /* Stats cards */
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: 0.75rem;
  }

  @media (max-width: 768px) {
    .reports-page {
      padding: 1rem;
    }
  }

  @media (max-width: 576px) {
    .reports-page {
      padding: 0.75rem;
    }
  }
</style>
