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
            v-if="latestReport"
            :number="latestReport.numberOfEntities?.toLocaleString() || 'N/A'"
            label="Patients"
            number-class="text-primary"
            help-text="Total number of patients included in the latest report"
          />
          <StatCard
            v-if="latestReport"
            :number="latestReport.numberOfSecondaryEntities?.toLocaleString() || 'N/A'"
            label="Samples"
            number-class="text-primary"
            help-text="Total number of samples included in the latest report"
          />
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

        <!-- Category Filter -->
        <div v-if="categories.length > 0" class="filter-section">
          <label class="filter-label">
            <i class="bi bi-funnel"></i>
            Filter by Category
          </label>
          <CategoryFilter v-model="selectedCategoryName" :categories="categories" />
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
  import { getResultPriority, getResultSummary } from '@/utils/reportResultUtils.js';
  import { ref, watch, computed, onMounted } from 'vue';
  import CategoryFilter from '@/components/CategoryFilter.vue';
  import { categoryService } from '@/services/categoryService.js';
  import { qualityCheckService } from '@/services/qualityCheckService.js';
  import { notificationService } from '@/services/notificationService.js';

  const showPasswordModal = ref(false);
  const isLoading = ref(true);
  const categories = ref([]);
  const qualityChecks = ref([]);
  const selectedCategoryName = ref(null);

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
      await Promise.all([
        reportStore.fetchLatestReport(),
        healthStore.checkHealth(),
        loadCategories(),
        loadQualityChecks(),
      ]);
    } catch (error) {
      console.error('Error loading dashboard data:', error);
    } finally {
      isLoading.value = false;
    }
  });

  const loadCategories = async () => {
    try {
      categories.value = await categoryService.getAll();
    } catch (error) {
      console.error('Failed to load categories:', error);
    }
  };

  const loadQualityChecks = async () => {
    try {
      const result = await qualityCheckService.getAll({ page: 0, size: 1000 });
      qualityChecks.value = result.items;
    } catch (error) {
      console.error('Failed to load quality checks:', error);
      notificationService.error('Load Failed', 'Unable to load quality checks. Please try again.');
    }
  };

  const categoryNameByCheckId = computed(() => {
    const map = {};
    qualityChecks.value.forEach((check) => {
      map[check.id] = check.category?.name || null;
    });
    return map;
  });

  const latestReport = computed(() => reportStore.latestReport);

  const filteredReport = computed(() => {
    if (!latestReport.value) return null;
    return {
      ...latestReport.value,
      results: filteredResults.value,
    };
  });

  const resultSummary = computed(() => getResultSummary(filteredReport.value));

  const successfulChecks = computed(() => {
    return resultSummary.value.passed;
  });

  const errorChecks = computed(() => {
    return resultSummary.value.failed;
  });

  const warningChecks = computed(() => {
    return resultSummary.value.warnings;
  });

  const filteredResults = computed(() => {
    if (!latestReport.value?.results) return [];

    if (!selectedCategoryName.value) {
      return latestReport.value.results;
    }

    if (selectedCategoryName.value === 'none') {
      return latestReport.value.results.filter(
        (result) => !categoryNameByCheckId.value[result.checkId]
      );
    }

    return latestReport.value.results.filter((result) => {
      const categoryName = categoryNameByCheckId.value[result.checkId];
      return categoryName === selectedCategoryName.value;
    });
  });

  const sortedResults = computed(() => {
    if (!filteredResults.value.length) return [];

    return [...filteredResults.value].sort((a, b) => {
      return (
        getResultPriority(filteredReport.value, a) - getResultPriority(filteredReport.value, b)
      );
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

  .filter-section {
    margin-bottom: var(--spacing-lg);
    padding: var(--spacing-md);
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
  }

  .filter-label {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--color-gray-600);
    margin-bottom: var(--spacing-sm);
  }

  .filter-label i {
    color: var(--color-primary);
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
