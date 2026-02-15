<template>
  <div class="dashboard-page">
    <PasswordChangeModal :is-visible="showPasswordModal" @close="closePasswordModal" />

    <PageHeader
      title="Overview"
      mobile-title="Overview"
      subtitle="View and validate the current Data Quality of your repository"
      icon="bi bi-pie-chart"
    />

    <div class="page-content">
      <HealthStatusBanner />

      <!-- Loading Spinner -->
      <div v-if="isLoading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status" style="width: 3rem; height: 3rem">
          <span class="visually-hidden">Loading...</span>
        </div>
        <p class="text-muted mt-3">Loading reports...</p>
      </div>

      <template v-else>
        <!-- Statistics Grid -->
        <div class="stats-grid mb-4">
          <StatCard
            :number="successfulChecks"
            label="Passed"
            number-class="text-success"
            help-text="Quality checks that passed without warnings or errors"
          />
          <StatCard
            :number="errorChecks"
            label="Errors"
            number-class="text-danger"
            help-text="Quality checks that exceeded the error threshold or encountered errors"
          />
          <StatCard
            :number="warningChecks"
            label="Warnings"
            number-class="text-warning"
            help-text="Quality checks that exceeded the warning threshold but not the error threshold"
          />
          <StatCard
            v-if="latestReport"
            :number="formatTimestamp(latestReport.generatedAt)"
            label="Generated At"
            number-class="text-primary"
            help-text="Timestamp when this quality report was generated"
          />
        </div>

        <!-- Generate Report Section -->
        <div v-if="latestReport" class="card mb-4 border-0 shadow-sm">
          <div class="card-header bg-white d-flex justify-content-between align-items-center">
            <div>
              <h5 class="mb-0">Latest Report</h5>
              <small class="text-muted"
                >Generated: {{ formatDate(latestReport.generatedAt) }}</small
              >
            </div>
            <ActionButton
              :loading="reportStore.isGenerating"
              :disabled="healthStore.healthStatus?.status !== 'UP'"
              icon="bi bi-plus"
              text="Generate Report"
              @click="generateReportWithReset"
            />
          </div>
        </div>

        <!-- No Reports State -->
        <div v-if="!latestReport" class="text-center py-5">
          <template v-if="reportStore.isGenerating">
            <div
              class="spinner-border text-primary"
              role="status"
              style="width: 3rem; height: 3rem"
            >
              <span class="visually-hidden">Generating report...</span>
            </div>
            <p class="text-muted mt-3">Generating your first report...</p>
          </template>
          <template v-else>
            <i class="bi bi-file-earmark-text-fill display-1 text-muted opacity-50"></i>
            <p class="text-muted mt-3">No Data Quality Reports available.</p>
            <ActionButton
              :disabled="healthStore.healthStatus?.status !== 'UP'"
              icon="bi bi-plus"
              text="Generate First Report"
              @click="generateReportWithReset"
            />
          </template>
        </div>

        <!-- Quality Checks Grid -->
        <div v-if="latestReport?.results" class="quality-checks-grid">
          <QualityCheckCard
            v-for="result in sortedResults"
            :key="getCheckKey(result)"
            :check="result"
            :total-entities="latestReport.numberOfEntities"
            :report-id="latestReport.id"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
  import HealthStatusBanner from '@/components/HealthStatusBanner.vue';
  import PasswordChangeModal from '@/components/PasswordChangeModal.vue';
  import PageHeader from '@/components/PageHeader.vue';
  import StatCard from '@/components/StatCard.vue';
  import QualityCheckCard from '@/components/QualityCheckCard.vue';
  import ActionButton from '@/components/ActionButton.vue';
  import { useUserStore } from '@/stores/userStore.js';
  import { useHealthStore } from '@/stores/healthStore.js';
  import { useReportStore } from '@/stores/reportStore.js';
  import { ref, watch, computed, onMounted } from 'vue';

  const showPasswordModal = ref(false);
  const isLoading = ref(true);

  const userStore = useUserStore();
  const healthStore = useHealthStore();
  const reportStore = useReportStore();

  watch(showPasswordModal, (newValue) => {
    if (!newValue) {
      userStore.initializeDefaultPasswordStatus();
    }
  });

  const closePasswordModal = () => {
    showPasswordModal.value = false;
  };

  // Load reports on page mount
  onMounted(async () => {
    isLoading.value = true;
    try {
      await Promise.all([reportStore.fetchLatestReport(), healthStore.checkHealth()]);
    } catch (error) {
      console.error('Error loading dashboard data:', error);
    } finally {
      isLoading.value = false;
    }
  });


  const latestReport = computed(() => reportStore.latestReport);

  const calculatePercentage = (result) => {
    const total = latestReport.value?.numberOfEntities || 1;
    return (result.obfuscatedValue / total) * 100;
  };

  const successfulChecks = computed(() => {
    if (!latestReport.value?.results) return 0;
    return latestReport.value.results.filter((result) => {
      const percentage = calculatePercentage(result);
      return percentage < result.warningThreshold && !result.error;
    }).length;
  });

  const errorChecks = computed(() => {
    if (!latestReport.value?.results) return 0;
    return latestReport.value.results.filter((result) => {
      const percentage = calculatePercentage(result);
      return percentage >= result.errorThreshold || result.error;
    }).length;
  });

  const warningChecks = computed(() => {
    if (!latestReport.value?.results) return 0;
    return latestReport.value.results.filter((result) => {
      const percentage = calculatePercentage(result);
      return (
        percentage >= result.warningThreshold && percentage < result.errorThreshold && !result.error
      );
    }).length;
  });

  const sortedResults = computed(() => {
    if (!latestReport.value?.results) return [];

    return [...latestReport.value.results].sort((a, b) => {
      const aPercentage = calculatePercentage(a);
      const bPercentage = calculatePercentage(b);

      // Determine status for a
      let aStatus;
      if (aPercentage >= a.errorThreshold || a.error) {
        aStatus = 0; // Error
      } else if (aPercentage >= a.warningThreshold && aPercentage < a.errorThreshold && !a.error) {
        aStatus = 1; // Warning
      } else {
        aStatus = 2; // Passed
      }

      // Determine status for b
      let bStatus;
      if (bPercentage >= b.errorThreshold || b.error) {
        bStatus = 0; // Error
      } else if (bPercentage >= b.warningThreshold && bPercentage < b.errorThreshold && !b.error) {
        bStatus = 1; // Warning
      } else {
        bStatus = 2; // Passed
      }

      return aStatus - bStatus;
    });
  });

  const getCheckKey = (result) => {
    return result.checkId + '_' + (result.stratum || 'all');
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const formatTimestamp = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const generateReportWithReset = async () => {
    await reportStore.generateReport();
  };
</script>

<style scoped>
  .dashboard-page {
    min-height: 100%;
    padding: var(--spacing-xl);
  }

  .page-content {
    width: 100%;
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: var(--spacing-lg);
    margin-bottom: var(--spacing-xl);
  }

  .quality-checks-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--spacing-md);
    align-items: start;
  }

  .quality-checks-grid > * {
    min-height: 100px;
  }

  @media (min-width: 992px) {
    .quality-checks-grid > * {
      height: 320px;
    }
  }

  @media (max-width: 768px) {
    .dashboard-page {
      padding: var(--spacing-md);
    }
    .stats-grid {
      grid-template-columns: repeat(2, 1fr);
      gap: var(--spacing-md);
    }
  }

  @media (max-width: 576px) {
    .dashboard-page {
      padding: 0.75rem;
    }
    .stats-grid {
      grid-template-columns: 1fr;
      gap: 0.75rem;
    }
    .quality-checks-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
