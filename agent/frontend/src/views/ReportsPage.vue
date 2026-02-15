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
      <div class="page-actions">
        <ActionButton
          :loading="reportStore.isGenerating"
          icon="bi bi-plus"
          text="Generate Report"
          @click="generateReport"
        />
      </div>

      <!-- Stats Cards -->
      <div class="stats-grid mb-3 mb-md-4">
        <StatCard
          :number="reportStore.pagination.totalElements"
          label="Total Reports"
          number-class="text-dark"
        />
        <StatCard :number="latestReportTime" label="Latest Report" number-class="text-primary" />
      </div>

      <!-- Reports table -->
      <ReportsTable
        :loading="reportStore.isLoading"
        :reports="sortedReports"
        :total-elements="reportStore.pagination.totalElements"
        :total-pages="reportStore.pagination.totalPages"
        :current-page="reportStore.pagination.page"
        @page-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
  import { computed, onMounted, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import PageHeader from '@/components/PageHeader.vue';
  import ReportsTable from '@/components/ReportsTable.vue';
  import StatCard from '@/components/StatCard.vue';
  import ActionButton from '@/components/ActionButton.vue';
  import { useReportStore } from '@/stores/reportStore.js';

  const route = useRoute();
  const router = useRouter();
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

  const getPageFromUrl = () => {
    const pageParam = route.query.page;
    const page = parseInt(pageParam, 10);
    return isNaN(page) || page < 0 ? 0 : page;
  };

  const handlePageChange = (page) => {
    router.replace({ query: { ...route.query, page: page.toString() } });
  };

  watch(
    () => route.query.page,
    () => {
      reportStore.fetchReports({ page: getPageFromUrl(), size: reportStore.pagination.size });
    }
  );

  onMounted(() => {
    reportStore.fetchReports({ page: getPageFromUrl(), size: reportStore.pagination.size });
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

  .page-actions {
    display: flex;
    justify-content: flex-end;
    margin-bottom: var(--spacing-md);
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
