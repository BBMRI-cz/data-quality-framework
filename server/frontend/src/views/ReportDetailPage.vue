<template>
  <div class="report-detail-page">
    <PageHeader
      :title="`Report ${report?.id || ''}`"
      :mobile-title="`Report ${report?.id || ''}`"
      subtitle="Detailed view of data quality report"
      icon="bi bi-file-earmark-text-fill"
    />

    <div class="page-content">
      <!-- Back button -->
      <div class="mb-3">
        <button class="btn btn-outline-secondary btn-sm" @click="goBack">
          <i class="bi bi-arrow-left me-2"></i>Back to Reports
        </button>
      </div>

      <!-- Loading state -->
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status" style="width: 3rem; height: 3rem">
          <span class="visually-hidden">Loading...</span>
        </div>
        <p class="text-muted mt-3">Loading report details...</p>
      </div>

      <!-- Error state -->
      <div v-else-if="error" class="alert alert-danger">
        <i :class="`${getStatusIcon(CheckStatus.FAILED)} me-2`"></i>
        {{ error }}
      </div>

      <!-- Report details -->
      <template v-else-if="report">
        <!-- Stats Cards -->
        <div class="stats-grid mb-4">
          <StatsCard
            :label="'Total Checks'"
            :value="report.results?.length || 0"
            icon="bi bi-list-check"
            icon-color="#6c757d"
            icon-bg-color="#e7e7e7"
          />
          <StatsCard
            :label="'Received Date'"
            :value="formatTimestamp(report.timestamp)"
            icon="bi bi-clock-history"
            icon-color="#0dcaf0"
            icon-bg-color="#cff4fc"
          />
          <StatsCard
            :label="'Passed'"
            :value="countPassed()"
            :icon="getStatusIcon(CheckStatus.PASSED)"
            :icon-color="getStatusColor(CheckStatus.PASSED)"
            :icon-bg-color="getStatusBgColor(CheckStatus.PASSED)"
          />
          <StatsCard
            :label="'Warnings'"
            :value="countWarnings()"
            :icon="getStatusIcon(CheckStatus.WARNING)"
            :icon-color="getStatusColor(CheckStatus.WARNING)"
            :icon-bg-color="getStatusBgColor(CheckStatus.WARNING)"
          />
          <StatsCard
            :label="'Errors'"
            :value="countErrors()"
            :icon="getStatusIcon(CheckStatus.FAILED)"
            :icon-color="getStatusColor(CheckStatus.FAILED)"
            :icon-bg-color="getStatusBgColor(CheckStatus.FAILED)"
          />
        </div>

        <!-- Privacy Note -->
        <AppCallout type="info" icon="bi-shield-lock" class="mb-3">
          <small>
            Results in this report are obfuscated using differential privacy to protect sensitive
            information.
            <a
              href="https://bbmri-cz.github.io/data-quality-framework/user/privacy.html"
              target="_blank"
              rel="noopener noreferrer"
              class="fw-semibold"
              >Learn more</a
            >.
          </small>
        </AppCallout>

        <!-- Category Filter -->
        <div class="mb-4">
          <div class="filter-label">Category:</div>
          <CategoryFilter v-model="selectedCategory" :categories="categories" />
        </div>

        <div class="mb-4">
          <div class="filter-label">Status:</div>
          <CategoryFilter v-model="selectedStatus" :categories="statuses" />
        </div>

        <!-- Results Section -->
        <div class="card border-0 shadow-sm">
          <div class="card-body">
            <div v-if="filteredResults.length === 0" class="text-center py-4 text-muted">
              <i class="bi bi-inbox fs-1 d-block mb-2 opacity-50"></i>
              <p class="mb-0">No results available</p>
            </div>
            <div v-else class="results-container">
              <div
                v-for="result in filteredResults"
                :id="getCheckIdKey(result)"
                :key="getCheckIdKey(result)"
                :class="['result-card', 'card', 'mb-3', getResultClass(result), 'cursor-pointer']"
                @click="navigateToCheckDetail(result)"
              >
                <div class="card-body">
                  <div class="d-flex justify-content-between align-items-start">
                    <div class="flex-grow-1">
                      <h6 class="card-title mb-2">{{ getCheckName(result) }}</h6>
                      <p v-if="getCheckDescription(result)" class="check-description mb-2">
                        {{ getCheckDescription(result) }}
                      </p>
                      <p class="check-hash mb-2">
                        <span class="hash-label">Hash:</span>
                        <code class="hash-value">{{ result.hash }}</code>
                      </p>
                      <div class="result-details">
                        <div class="detail-row">
                          <span class="detail-label">Occurrence Rate:</span>
                          <span class="detail-value">{{ getResultValue(result) }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
  import { ref, onMounted, nextTick, computed } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { useHead } from '@unhead/vue';
  import PageHeader from '@/components/PageHeader.vue';
  import StatsCard from '@/components/StatsCard.vue';
  import AppCallout from '@/components/AppCallout.vue';
  import CategoryFilter from '@/components/CategoryFilter.vue';
  import { apiService } from '@/services/apiService.js';
  import {
    getCheckStatus,
    CheckStatus,
    getStatusIcon,
    getStatusColor,
    getStatusBgColor,
  } from '@/utils/qualityCheckUtils.js';

  const route = useRoute();
  const router = useRouter();

  const loading = ref(true);
  const error = ref(null);
  const report = ref(null);
  useHead({
    title: computed(() => (report.value?.id ? `Report ${report.value.id}` : 'Report Detail')),
  });
  const qualityCheckMap = ref(new Map());
  const selectedCategory = ref(null);
  const selectedStatus = ref(null);
  const statuses = [CheckStatus.PASSED, CheckStatus.WARNING, CheckStatus.FAILED];

  const categories = computed(() => {
    const cats = new Set();
    qualityCheckMap.value.forEach((check) => {
      if (check.category && check.category.name) {
        cats.add(check.category.name);
      } else {
        cats.add('No Category');
      }
    });
    return Array.from(cats).sort();
  });

  const filteredResults = computed(() => {
    if (!report.value?.results) return [];

    const filtered = report.value.results.filter((result) => {
      const check = qualityCheckMap.value.get(result.hash);

      if (selectedCategory.value) {
        const categoryName = check?.category?.name || 'No Category';
        if (categoryName !== selectedCategory.value) {
          return false;
        }
      }

      if (selectedStatus.value) {
        const status = getCheckStatus(result, check);
        if (status !== selectedStatus.value) {
          return false;
        }
      }

      return true;
    });

    const statusOrder = {
      [CheckStatus.FAILED]: 0,
      [CheckStatus.WARNING]: 1,
      [CheckStatus.PASSED]: 2,
    };

    return [...filtered].sort((a, b) => {
      const order = (item) => {
        const check = qualityCheckMap.value.get(item.hash);
        const status = getCheckStatus(item, check);
        return statusOrder[status] ?? 999;
      };

      return order(a) - order(b) || a.hash.localeCompare(b.hash);
    });
  });

  const goBack = () => {
    router.push('/reports');
  };

  const navigateToCheckDetail = (result) => {
    const url = router.resolve(`/quality-checks/${result.hash}`).href;
    window.open(url, '_blank');
  };

  function getCheckIdKey(result) {
    return result.hash;
  }

  function getCheckName(result) {
    const check = qualityCheckMap.value.get(result.hash);
    return check?.name || 'No Name';
  }

  function getCheckDescription(result) {
    const check = qualityCheckMap.value.get(result.hash);
    return check?.description || 'No Description';
  }

  const countErrors = () => {
    if (!report.value?.results) return 0;
    return report.value.results.filter((result) => {
      const check = qualityCheckMap.value.get(result.hash);
      if (!check) return false;
      return getCheckStatus(result, check) === CheckStatus.FAILED;
    }).length;
  };

  const countWarnings = () => {
    if (!report.value?.results) return 0;
    return report.value.results.filter((result) => {
      const check = qualityCheckMap.value.get(result.hash);
      if (!check) return false;
      return getCheckStatus(result, check) === CheckStatus.WARNING;
    }).length;
  };

  const countPassed = () => {
    if (!report.value?.results) return 0;
    return report.value.results.filter((result) => {
      const check = qualityCheckMap.value.get(result.hash);
      if (!check) return false;
      return getCheckStatus(result, check) === CheckStatus.PASSED;
    }).length;
  };

  const getResultClass = (result) => {
    const check = qualityCheckMap.value.get(result.hash);
    if (!check) return 'bg-secondary';

    const status = getCheckStatus(result, check);
    if (status === CheckStatus.FAILED) {
      return 'bg-danger';
    } else if (status === CheckStatus.WARNING) {
      return 'bg-warning';
    }
    return 'bg-success';
  };

  function getResultValue(result) {
    if (result.result == null) return 'N/A';
    return `${(result.result * 100).toFixed(1)}%`;
  }

  function getThresholds(result) {
    const check = qualityCheckMap.value.get(result.hash);
    if (!check) return null;
    return {
      warning: check.warningThreshold,
      error: check.errorThreshold,
    };
  }

  function formatTimestamp(timestamp) {
    if (!timestamp) return 'N/A';

    try {
      const date = new Date(timestamp);
      const now = new Date();
      const diffMs = now - date;
      const diffMins = Math.floor(diffMs / 60000);
      const diffHours = Math.floor(diffMs / 3600000);
      const diffDays = Math.floor(diffMs / 86400000);

      // If less than 1 hour ago, show "X minutes ago"
      if (diffMins < 60) {
        return diffMins <= 1 ? 'Just now' : `${diffMins} min ago`;
      }

      // If less than 24 hours ago, show "X hours ago"
      if (diffHours < 24) {
        return diffHours === 1 ? '1 hour ago' : `${diffHours} hours ago`;
      }

      // If less than 7 days ago, show "X days ago"
      if (diffDays < 7) {
        return diffDays === 1 ? '1 day ago' : `${diffDays} days ago`;
      }

      // Otherwise show formatted date
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
      });
    } catch (err) {
      return 'Invalid date';
    }
  }

  const scrollToCheck = async () => {
    if (route.hash) {
      // Remove the # from the hash
      const checkId = route.hash.substring(1);

      // Wait for DOM to be fully rendered
      await nextTick();

      // Small additional delay to ensure everything is rendered
      setTimeout(() => {
        const element = document.getElementById(checkId);
        if (element) {
          element.scrollIntoView({
            behavior: 'smooth',
            block: 'center',
          });

          // Add a highlight animation
          element.classList.add('highlight-check');
          setTimeout(() => {
            element.classList.remove('highlight-check');
          }, 2000);
        }
      }, 100);
    }
  };

  onMounted(async () => {
    try {
      loading.value = true;
      const reportId = route.params.id;

      // Fetch report and quality checks in parallel
      const [reportData, checksData] = await Promise.all([
        apiService.getReport(reportId),
        apiService.getQualityChecks(),
      ]);

      report.value = reportData;

      // Handle HAL format response for quality checks
      const checks =
        checksData._embedded?.qualityChecks || (Array.isArray(checksData) ? checksData : []);
      qualityCheckMap.value = new Map(checks.map((check) => [check.hash, check]));

      // Scroll to the specific check if hash is present
      await scrollToCheck();
    } catch (err) {
      error.value = `Failed to load report: ${err.message || 'Unknown error'}`;
    } finally {
      loading.value = false;
    }
  });
</script>

<style scoped>
  .report-detail-page {
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
  }

  .results-container {
    display: flex;
    flex-direction: column;
  }

  .cursor-pointer {
    cursor: pointer;
  }

  .result-card {
    transition:
      transform var(--transition-base),
      box-shadow var(--transition-base);
    border: 1px solid var(--color-gray-200);
    scroll-margin-top: 100px;
  }

  .result-card:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }

  .result-card.bg-success {
    background-color: var(--bg-card) !important;
    border-left: 4px solid var(--color-success);
  }

  .result-card.bg-warning {
    background-color: var(--bg-card) !important;
    border-left: 4px solid var(--color-warning);
  }

  .result-card.bg-danger {
    background-color: var(--bg-card) !important;
    border-left: 4px solid var(--color-danger);
  }

  .highlight-check {
    animation: highlightPulse 2s ease-in-out;
  }

  .check-description {
    color: var(--color-gray-500);
    font-size: 0.875rem;
    font-style: italic;
  }

  .check-hash {
    color: var(--color-gray-600);
    font-size: 0.8rem;
    margin-bottom: 0.5rem;
  }

  .hash-label {
    font-weight: 600;
    margin-right: 0.5rem;
  }

  .hash-value {
    background-color: var(--color-gray-100);
    padding: 0.2rem 0.4rem;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    color: var(--color-gray-700);
    font-family: var(--font-mono), monospace;
  }

  .filter-label {
    font-size: 0.875rem;
    color: #6c757d;
    font-weight: 500;
    margin-bottom: 0.5rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  @keyframes highlightPulse {
    0%,
    100% {
      box-shadow: 0 0 0 rgba(102, 126, 234, 0);
    }
    50% {
      box-shadow: 0 0 20px 5px rgba(102, 126, 234, 0.5);
    }
  }

  .result-details {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-sm);
  }

  .detail-row {
    display: flex;
    gap: var(--spacing-sm);
    font-size: 0.9rem;
  }

  .detail-label {
    font-weight: 600;
    color: var(--color-gray-500);
    min-width: 120px;
  }

  .detail-value {
    color: var(--color-gray-900);
  }

  .patient-ids-section {
    background-color: var(--color-gray-50);
    padding: var(--spacing-md);
    border-radius: var(--radius-sm);
  }

  .patient-cell {
    padding: var(--spacing-sm);
    width: 16.66%;
  }

  .patient-link {
    text-decoration: none;
    color: var(--color-primary);
    padding: var(--spacing-xs) var(--spacing-sm);
    border-radius: var(--radius-sm);
    transition: background-color var(--transition-base);
    display: inline-block;
  }

  .patient-link:hover {
    background-color: var(--bg-hover);
    text-decoration: underline;
  }

  .badge {
    font-weight: 500;
    padding: var(--spacing-sm) var(--spacing-md);
    font-size: 0.875rem;
  }

  @media (max-width: 768px) {
    .report-detail-page {
      padding: var(--spacing-md);
    }

    .stats-grid {
      grid-template-columns: repeat(2, 1fr);
    }

    .detail-label {
      min-width: 100px;
      font-size: 0.85rem;
    }

    .detail-value {
      font-size: 0.85rem;
    }
  }

  @media (max-width: 576px) {
    .report-detail-page {
      padding: 0.75rem;
    }

    .stats-grid {
      grid-template-columns: 1fr;
    }

    .patient-cell {
      width: 33.33%;
    }
  }
</style>
