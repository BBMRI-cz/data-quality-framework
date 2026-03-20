<template>
  <div class="agent-card card border-0 shadow-sm" @click="handleCardClick">
    <div class="card-body">
      <div class="card-content">
        <!-- Left: Agent Information -->
        <div class="agent-info">
          <div class="agent-header">
            <i class="bi bi-database-fill-gear agent-icon"></i>
            <h6 class="agent-name">{{ agent.name }}</h6>
          </div>
          <p v-if="agent.description" class="agent-description">
            {{ agent.description }}
          </p>
        </div>

        <!-- Right: Status Indicator and Actions -->
        <div class="card-controls">
          <i
            class="status-indicator"
            :class="[agentStatusIcon, agentStatusClass]"
            :title="agentStatusTooltip"
          ></i>

          <div class="action-buttons">
            <button
              class="action-btn expand-btn"
              :title="expandButtonTitle"
              :aria-expanded="isExpanded"
            >
              <i :class="expandIconClass"></i>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Expanded Content -->
    <Transition name="expand">
      <div v-if="isExpanded" class="expanded-section" @click.stop>
        <div class="check-results-container">
          <div class="results-header">
            <h6>Quality Check Results</h6>
            <span class="text-muted small">{{ checkCountText }}</span>
          </div>

          <div v-if="hasNoResults" class="no-results">
            <i class="bi bi-info-circle"></i>
            <span>No quality check results available</span>
          </div>

          <div v-else class="check-list">
            <div
              v-for="check in sortedCheckResults"
              :key="check.hash"
              class="check-item"
              :class="getCheckStatusClass(check)"
              role="button"
              tabindex="0"
              :title="`View details for ${check.checkName}`"
              @click="navigateToCheckDetail(check)"
              @keydown.enter="navigateToCheckDetail(check)"
              @keydown.space.prevent="navigateToCheckDetail(check)"
            >
              <div class="check-item-info">
                <i :class="getCheckStatusIcon(check)"></i>
                <div class="check-item-text">
                  <span class="check-item-name">{{ check.checkName }}</span>
                  <span v-if="check.category" class="check-item-category">
                    {{ check.category }}
                  </span>
                </div>
              </div>
              <span class="check-item-result">{{ formatCheckResult(check.result) }}</span>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
  import { computed, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { CheckStatus } from '@/utils/qualityCheckUtils.js';

  const router = useRouter();

  const props = defineProps({
    agent: {
      type: Object,
      required: true,
    },
    reports: {
      type: Array,
      required: true,
      default: () => [],
    },
    qualityCheckMap: {
      type: Map,
      required: true,
    },
    selectedCategory: {
      type: String,
      default: null,
    },
    selectedGroup: {
      type: String,
      default: null,
    },
  });

  const isExpanded = ref(false);

  // Get the latest report for this agent
  const latestReport = computed(() => {
    const agentReports = props.reports.filter((r) => (r.agentId || r.agent?.id) === props.agent.id);

    if (agentReports.length === 0) return null;

    return agentReports.reduce((latest, current) => {
      const latestDate = new Date(latest.timestamp);
      const currentDate = new Date(current.timestamp);
      return currentDate > latestDate ? current : latest;
    });
  });

  // Get check results for this agent
  const checkResults = computed(() => {
    if (!latestReport.value?.results) return [];

    return latestReport.value.results
      .map((result) => {
        const qualityCheck = props.qualityCheckMap.get(result.hash);
        if (!qualityCheck) return null;

        return {
          hash: result.hash,
          checkName: qualityCheck.name || qualityCheck.cql || result.hash,
          category: qualityCheck.category?.name,
          result: normalizeResultValue(result.result),
          qualityCheck: qualityCheck,
        };
      })
      .filter(Boolean);
  });

  // Filter check results by selected category
  const filteredCheckResults = computed(() => {
    if (!props.selectedCategory) {
      return checkResults.value;
    }

    return checkResults.value.filter((check) => {
      const categoryName = check.category || 'No Category';
      return categoryName === props.selectedCategory;
    });
  });

  // Sort check results by status (worst first)
  const sortedCheckResults = computed(() => {
    return [...filteredCheckResults.value].sort((a, b) => {
      const statusA = getResultStatus(a.result, a.qualityCheck);
      const statusB = getResultStatus(b.result, b.qualityCheck);

      const statusOrder = {
        [CheckStatus.FAILED]: 0,
        [CheckStatus.WARNING]: 1,
        [CheckStatus.PASSED]: 2,
      };

      const orderDiff = statusOrder[statusA] - statusOrder[statusB];
      if (orderDiff !== 0) return orderDiff;

      return a.checkName.localeCompare(b.checkName);
    });
  });

  // Calculate agent statistics
  const agentStats = computed(() => {
    const stats = {
      errors: 0,
      warnings: 0,
      totalChecks: filteredCheckResults.value.length,
    };

    filteredCheckResults.value.forEach((check) => {
      const status = getResultStatus(check.result, check.qualityCheck);
      if (status === CheckStatus.FAILED) {
        stats.errors++;
      } else if (status === CheckStatus.WARNING) {
        stats.warnings++;
      }
    });

    return stats;
  });

  const hasNoResults = computed(() => filteredCheckResults.value.length === 0);

  const checkCountText = computed(
    () =>
      `${filteredCheckResults.value.length} check${filteredCheckResults.value.length !== 1 ? 's' : ''}`
  );

  const expandButtonTitle = computed(() => (isExpanded.value ? 'Collapse' : 'Expand'));

  const expandIconClass = computed(() =>
    isExpanded.value ? 'bi bi-chevron-up' : 'bi bi-chevron-down'
  );

  // Agent overall status
  const agentOverallStatus = computed(() => {
    if (agentStats.value.errors > 0) {
      return 'error';
    } else if (agentStats.value.warnings > 0) {
      return 'warning';
    }
    return 'success';
  });

  const agentStatusClass = computed(() => ({
    'status-error': agentOverallStatus.value === 'error',
    'status-warning': agentOverallStatus.value === 'warning',
    'status-success': agentOverallStatus.value === 'success',
  }));

  const agentStatusIcon = computed(() => {
    switch (agentOverallStatus.value) {
      case 'error':
        return 'bi bi-exclamation-triangle-fill';
      case 'warning':
        return 'bi bi-exclamation-circle-fill';
      case 'success':
      default:
        return 'bi bi-check-circle-fill';
    }
  });

  const agentStatusTooltip = computed(() => {
    const parts = [];
    if (agentStats.value.errors > 0) {
      parts.push(`${agentStats.value.errors} error${agentStats.value.errors !== 1 ? 's' : ''}`);
    }
    if (agentStats.value.warnings > 0) {
      parts.push(
        `${agentStats.value.warnings} warning${agentStats.value.warnings !== 1 ? 's' : ''}`
      );
    }

    const totalChecks = agentStats.value.totalChecks;
    const categoryText = props.selectedCategory ? ` in ${props.selectedCategory}` : '';

    if (parts.length === 0) {
      return `All ${totalChecks} check${totalChecks !== 1 ? 's' : ''} passed${categoryText}`;
    }
    return (
      parts.join(', ') +
      ` out of ${totalChecks} check${totalChecks !== 1 ? 's' : ''}${categoryText}`
    );
  });

  // Helper Functions
  function normalizeResultValue(raw) {
    if (typeof raw !== 'number' || isNaN(raw)) return 0;
    return raw > 1 ? Math.min(raw / 100, 1) : Math.max(raw, 0);
  }

  function getResultStatus(result, qualityCheck) {
    const percentage = result <= 1 ? result * 100 : result;

    if (percentage > qualityCheck.errorThreshold) {
      return CheckStatus.FAILED;
    } else if (percentage > qualityCheck.warningThreshold) {
      return CheckStatus.WARNING;
    }
    return CheckStatus.PASSED;
  }

  function getCheckStatusClass(check) {
    const status = getResultStatus(check.result, check.qualityCheck);
    return {
      'status-error': status === CheckStatus.FAILED,
      'status-warning': status === CheckStatus.WARNING,
      'status-success': status === CheckStatus.PASSED,
    };
  }

  function getCheckStatusIcon(check) {
    const status = getResultStatus(check.result, check.qualityCheck);
    switch (status) {
      case CheckStatus.FAILED:
        return 'bi bi-x-circle-fill';
      case CheckStatus.WARNING:
        return 'bi bi-exclamation-circle-fill';
      case CheckStatus.PASSED:
      default:
        return 'bi bi-check-circle-fill';
    }
  }

  function formatCheckResult(result) {
    const percentage = result <= 1 ? result * 100 : result;
    return `${percentage.toFixed(1)}%`;
  }

  function handleCardClick() {
    isExpanded.value = !isExpanded.value;
  }

  function navigateToCheckDetail(check) {
    router.push({ name: 'QualityCheckDetail', params: { hash: check.hash } });
  }
</script>

<style scoped>
  /* Card Container */
  .agent-card {
    transition:
      transform var(--transition-base),
      box-shadow var(--transition-base);
    cursor: pointer;
    margin-bottom: var(--spacing-sm);
  }

  .agent-card:hover {
    transform: translateX(2px);
    box-shadow: var(--shadow-md) !important;
  }

  .agent-card .card-body {
    padding: var(--spacing-md) var(--spacing-lg);
  }

  /* Card Content Layout */
  .card-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--spacing-lg);
  }

  /* Agent Information */
  .agent-info {
    flex: 1;
    min-width: 0;
  }

  .agent-header {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    margin-bottom: var(--spacing-xs);
  }

  .agent-icon {
    font-size: 1.5rem;
    color: var(--color-primary);
    flex-shrink: 0;
  }

  .agent-name {
    font-weight: 600;
    font-size: 1.1rem;
    color: var(--color-gray-900);
    margin: 0;
  }

  .agent-description {
    font-size: 0.875rem;
    color: var(--color-gray-600);
    line-height: 1.4;
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    line-clamp: 2;
  }

  /* Card Controls */
  .card-controls {
    display: flex;
    align-items: center;
    gap: var(--spacing-md);
    flex-shrink: 0;
  }

  /* Status Indicator */
  .status-indicator {
    font-size: 1.5rem;
    transition: all var(--transition-base);
  }

  .status-indicator.status-error {
    color: var(--color-danger);
  }

  .status-indicator.status-warning {
    color: var(--color-warning);
  }

  .status-indicator.status-success {
    color: var(--color-success);
  }

  /* Action Buttons */
  .action-buttons {
    display: flex;
    gap: var(--spacing-xs);
    align-items: center;
  }

  .action-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border: none;
    background: transparent;
    color: var(--color-gray-500);
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all var(--transition-base);
    font-size: 1rem;
  }

  .action-btn:hover {
    background-color: rgba(102, 126, 234, 0.1);
    color: var(--color-primary);
  }

  /* Expanded Section */
  .expanded-section {
    border-top: 1px solid var(--color-gray-200);
    background-color: var(--color-gray-50);
  }

  .check-results-container {
    padding: var(--spacing-md) var(--spacing-lg);
  }

  .results-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--spacing-sm);
    padding-bottom: var(--spacing-sm);
    border-bottom: 2px solid var(--color-gray-300);
  }

  .results-header h6 {
    font-size: 0.9rem;
    font-weight: 600;
    color: var(--color-gray-700);
    margin: 0;
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
  }

  /* Check List */
  .check-list {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-xs);
  }

  .check-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.65rem 0.85rem;
    background-color: var(--bg-card);
    border-radius: var(--radius-md);
    border-left: 3px solid var(--color-gray-300);
    transition: all var(--transition-base);
    cursor: pointer;
  }

  .check-item:hover {
    transform: translateX(3px);
    box-shadow: var(--shadow-sm);
    background-color: var(--color-gray-50);
  }

  .check-item:focus {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }

  .check-item.status-error {
    border-left-color: var(--color-danger);
  }

  .check-item.status-warning {
    border-left-color: var(--color-warning);
  }

  .check-item.status-success {
    border-left-color: var(--color-success);
  }

  .check-item-info {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    flex: 1;
    min-width: 0;
  }

  .check-item-info i {
    font-size: 0.95rem;
    flex-shrink: 0;
  }

  .check-item.status-error .check-item-info i {
    color: var(--color-danger);
  }

  .check-item.status-warning .check-item-info i {
    color: var(--color-warning);
  }

  .check-item.status-success .check-item-info i {
    color: var(--color-success);
  }

  .check-item-text {
    display: flex;
    flex-direction: column;
    gap: 0.125rem;
    min-width: 0;
  }

  .check-item-name {
    font-weight: 500;
    font-size: 0.875rem;
    color: var(--color-gray-900);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .check-item-category {
    font-size: 0.75rem;
    color: var(--color-gray-500);
    font-style: italic;
  }

  .check-item-result {
    font-size: 0.85rem;
    font-weight: 600;
    color: var(--color-gray-600);
    white-space: nowrap;
    margin-left: var(--spacing-sm);
  }

  /* Transition Animations */
  .expand-enter-active,
  .expand-leave-active {
    transition: all var(--transition-smooth);
    overflow: hidden;
  }

  .expand-enter-from,
  .expand-leave-to {
    max-height: 0;
    opacity: 0;
  }

  .expand-enter-to,
  .expand-leave-from {
    max-height: 2000px;
    opacity: 1;
  }

  /* Responsive Design */
  @media (max-width: 992px) {
    .card-content {
      flex-direction: column;
      align-items: flex-start;
      gap: var(--spacing-md);
    }

    .agent-info {
      width: 100%;
    }

    .card-controls {
      width: 100%;
      justify-content: space-between;
    }
  }

  @media (max-width: 576px) {
    .agent-card .card-body {
      padding: 0.875rem var(--spacing-md);
    }

    .agent-name {
      font-size: 1rem;
    }

    .agent-description {
      font-size: 0.8rem;
    }

    .status-indicator {
      font-size: 1.25rem;
    }

    .action-btn {
      width: 32px;
      height: 32px;
    }

    .check-item-name {
      font-size: 0.8rem;
    }

    .check-item-category {
      font-size: 0.7rem;
    }
  }
</style>
