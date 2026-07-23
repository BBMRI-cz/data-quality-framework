<template>
  <div class="quality-check-card card border-0 shadow-sm" @click="handleCardClick">
    <div class="card-body">
      <div class="card-content">
        <!-- Left: Check Information -->
        <div class="check-info">
          <div class="check-header">
            <span class="category-badge badge border" :style="categoryBadgeStyle">
              {{ categoryName }}
            </span>
            <h6 class="check-name" :title="checkTitle">
              {{ checkTitle }}
            </h6>
          </div>
          <p v-if="hasDescription" class="check-description" :title="qualityCheck.description">
            {{ truncatedDescription }}
          </p>
        </div>

        <!-- Right: Stats and Actions -->
        <div class="card-controls">
          <div class="stats-container">
            <div class="stat-item stat-error">
              <span class="stat-label">Errors</span>
              <span class="stat-value">{{ coverageBuckets.failed }}</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item stat-warning">
              <span class="stat-label">Warnings</span>
              <span class="stat-value">{{ coverageBuckets.warning }}</span>
            </div>
          </div>

          <div class="action-buttons">
            <router-link
              :to="{ name: 'QualityCheckDetail', params: { hash: qualityCheck.hash } }"
              class="action-btn"
              title="Edit quality check"
              @click.stop
            >
              <i class="bi bi-pencil-square"></i>
            </router-link>
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
        <div class="agent-results-container">
          <div class="results-header">
            <h6>Results per Site</h6>
            <span class="text-muted small">{{ agentCountText }}</span>
          </div>

          <div v-if="hasNoResults" class="no-results">
            <i class="bi bi-info-circle"></i>
            <span>No results available</span>
          </div>

          <div v-else class="agent-list">
            <div
              v-for="agent in sortedAgentResults"
              :key="agent.agentId"
              class="agent-item"
              :class="getAgentStatusClass(agent.result)"
              role="button"
              tabindex="0"
              :title="`View latest report from ${agent.agentName}`"
              @click="navigateToReport(agent)"
              @keydown.enter="navigateToReport(agent)"
              @keydown.space.prevent="navigateToReport(agent)"
            >
              <div class="agent-info">
                <i :class="getAgentStatusIcon(agent.result)"></i>
                <span class="agent-name">{{ agent.agentName }}</span>
              </div>
              <span class="agent-result">{{ formatAgentResult(agent.result) }}</span>
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

  // Router
  const router = useRouter();

  // Constants
  const DESCRIPTION_MAX_LENGTH = 80;

  // Props
  const props = defineProps({
    qualityCheck: {
      type: Object,
      required: true,
    },
    reports: {
      type: Array,
      default: () => [],
    },
    agents: {
      type: Array,
      default: () => [],
    },
  });

  // State
  const isExpanded = ref(false);

  // Computed - Basic Info
  const categoryName = computed(() => props.qualityCheck.category?.name || 'No Category');

  const checkTitle = computed(
    () => props.qualityCheck.name || props.qualityCheck.cql || props.qualityCheck.hash
  );

  const hasDescription = computed(() => Boolean(props.qualityCheck.description));

  const truncatedDescription = computed(() => {
    const desc = props.qualityCheck.description || '';
    if (desc.length <= DESCRIPTION_MAX_LENGTH) return desc;
    return `${desc.substring(0, DESCRIPTION_MAX_LENGTH)}...`;
  });

  const categoryBadgeStyle = computed(() => {
    const color = props.qualityCheck.category?.colorHex || 'var(--color-gray-500)';
    return {
      backgroundColor: `${color}20`,
      color: color,
      borderColor: `${color}40`,
    };
  });

  // Computed - Agent Mapping
  const agentMap = computed(() => {
    const map = new Map();
    props.agents.forEach((agent) => {
      map.set(agent.id, agent.name || 'Unknown Agent');
    });
    return map;
  });

  // Computed - Agent Results
  const agentResults = computed(() => {
    const resultsMap = new Map();

    props.reports.forEach((report) => {
      const result = report.results?.find((r) => r.hash === props.qualityCheck.hash);
      if (!result) return;

      const agentId = report.agentId || report.agent?.id || 'unknown';
      const resultValue = normalizeResultValue(result.result);

      // Keep the worst (highest) result for each agent
      const existing = resultsMap.get(agentId);
      if (!existing || existing.result < resultValue) {
        resultsMap.set(agentId, {
          agentId,
          agentName: agentMap.value.get(agentId) || agentId,
          result: resultValue,
          timestamp: report.timestamp,
        });
      }
    });

    return Array.from(resultsMap.values());
  });

  const sortedAgentResults = computed(() => {
    return [...agentResults.value].sort((a, b) => {
      const statusA = getResultStatus(a.result);
      const statusB = getResultStatus(b.result);

      const statusOrder = {
        [CheckStatus.FAILED]: 0,
        [CheckStatus.WARNING]: 1,
        [CheckStatus.PASSED]: 2,
      };

      const orderDiff = statusOrder[statusA] - statusOrder[statusB];
      if (orderDiff !== 0) return orderDiff;

      return a.agentName.localeCompare(b.agentName);
    });
  });

  const totalAgents = computed(() => agentResults.value.length);

  const hasNoResults = computed(() => totalAgents.value === 0);

  const agentCountText = computed(
    () => `${totalAgents.value} site${totalAgents.value !== 1 ? 's' : ''}`
  );

  // Computed - Agent to Report Mapping
  const agentLatestReports = computed(() => {
    const reportMap = new Map();

    props.reports.forEach((report) => {
      const agentId = report.agentId || report.agent?.id;
      if (!agentId) return;

      // Keep the latest report for each agent
      const existing = reportMap.get(agentId);
      if (!existing || new Date(report.timestamp) > new Date(existing.timestamp)) {
        reportMap.set(agentId, {
          id: report.id,
          timestamp: report.timestamp,
        });
      }
    });

    return reportMap;
  });

  // Computed - Coverage Statistics
  const coverageBuckets = computed(() => {
    return agentResults.value.reduce(
      (acc, agent) => {
        const status = getResultStatus(agent.result);
        if (status === CheckStatus.FAILED) {
          acc.failed += 1;
        } else if (status === CheckStatus.WARNING) {
          acc.warning += 1;
        } else {
          acc.passed += 1;
        }
        return acc;
      },
      { passed: 0, warning: 0, failed: 0 }
    );
  });

  // Computed - UI State
  const expandButtonTitle = computed(() => (isExpanded.value ? 'Collapse' : 'Expand'));

  const expandIconClass = computed(() =>
    isExpanded.value ? 'bi bi-chevron-up' : 'bi bi-chevron-down'
  );

  // Helper Functions
  /**
   * Normalize result value to a fraction between 0 and 1
   * @param {number|null|undefined} raw - The raw result value
   * @returns {number} Normalized value between 0 and 1
   */
  function normalizeResultValue(raw) {
    if (typeof raw !== 'number' || isNaN(raw)) return 0;
    // If value is greater than 1, assume it's a percentage and convert to fraction
    return raw > 1 ? Math.min(raw / 100, 1) : Math.max(raw, 0);
  }

  /**
   * Get the status of a result based on quality check thresholds
   * @param {number} result - Result value (fraction 0-1 or percentage 0-100)
   * @returns {string} CheckStatus enum value
   */
  function getResultStatus(result) {
    const percentage = result <= 1 ? result * 100 : result;

    if (percentage > props.qualityCheck.errorThreshold) {
      return CheckStatus.FAILED;
    } else if (percentage > props.qualityCheck.warningThreshold) {
      return CheckStatus.WARNING;
    }
    return CheckStatus.PASSED;
  }

  /**
   * Get CSS class object for agent item based on result status
   * @param {number} result - Result value
   * @returns {Object} CSS class object
   */
  function getAgentStatusClass(result) {
    const status = getResultStatus(result);
    return {
      'status-error': status === CheckStatus.FAILED,
      'status-warning': status === CheckStatus.WARNING,
      'status-success': status === CheckStatus.PASSED,
    };
  }

  /**
   * Get Bootstrap icon class for agent status
   * @param {number} result - Result value
   * @returns {string} Icon class name
   */
  function getAgentStatusIcon(result) {
    const status = getResultStatus(result);
    switch (status) {
      case CheckStatus.FAILED:
        return 'bi bi-x-circle-fill';
      case CheckStatus.WARNING:
        return 'bi bi-exclamation-triangle-fill';
      case CheckStatus.PASSED:
      default:
        return 'bi bi-check-circle-fill';
    }
  }

  /**
   * Format agent result for display
   * @param {number} result - Result value
   * @returns {string} Formatted result string
   */
  function formatAgentResult(result) {
    const percentage = result <= 1 ? result * 100 : result;
    return `${percentage.toFixed(1)}% of records`;
  }

  // Event Handlers
  /**
   * Toggle the expanded state of the card
   */
  function handleCardClick() {
    isExpanded.value = !isExpanded.value;
  }

  /**
   * Navigate to the latest report for a specific agent
   * @param {Object} agent - Agent object with agentId and other properties
   */
  function navigateToReport(agent) {
    const latestReport = agentLatestReports.value.get(agent.agentId);
    if (latestReport?.id) {
      router.push({ name: 'ReportDetail', params: { id: latestReport.id } });
    } else {
      console.warn(`No report found for agent: ${agent.agentName}`);
    }
  }
</script>

<style scoped>
  /* Card Container */
  .quality-check-card {
    transition:
      transform var(--transition-base),
      box-shadow var(--transition-base);
    cursor: pointer;
    margin-bottom: var(--spacing-sm);
  }

  .quality-check-card:hover {
    transform: translateX(2px);
    box-shadow: var(--shadow-md) !important;
  }

  .quality-check-card .card-body {
    padding: var(--spacing-md) var(--spacing-lg);
  }

  /* Card Content Layout */
  .card-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--spacing-lg);
  }

  /* Check Information */
  .check-info {
    flex: 1;
    min-width: 0;
  }

  .check-header {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    margin-bottom: var(--spacing-xs);
  }

  .category-badge {
    flex-shrink: 0;
    font-size: 0.7rem;
    padding: 0.2rem 0.5rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.3px;
    border-radius: var(--radius-sm);
  }

  .check-name {
    font-weight: 600;
    font-size: 1rem;
    color: var(--color-gray-900);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin: 0;
  }

  .check-description {
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
    gap: var(--spacing-lg);
    flex-shrink: 0;
  }

  /* Stats Container */
  .stats-container {
    display: flex;
    align-items: center;
    gap: var(--spacing-md);
    padding: var(--spacing-sm) var(--spacing-md);
    background-color: var(--color-gray-50);
    border-radius: var(--radius-md);
    border: 1px solid var(--color-gray-200);
  }

  .stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--spacing-xs);
  }

  .stat-label {
    font-size: 0.7rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    opacity: 0.7;
  }

  .stat-value {
    font-size: 1.25rem;
    font-weight: 700;
    line-height: 1;
  }

  .stat-error .stat-label,
  .stat-error .stat-value {
    color: var(--color-danger);
  }

  .stat-warning .stat-label,
  .stat-warning .stat-value {
    color: var(--color-warning);
  }

  .stat-divider {
    width: 1px;
    height: 2rem;
    background-color: var(--color-gray-300);
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
    text-decoration: none;
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

  .agent-results-container {
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

  /* Agent List */
  .agent-list {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-xs);
  }

  .agent-item {
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

  .agent-item:hover {
    transform: translateX(3px);
    box-shadow: var(--shadow-sm);
    background-color: var(--color-gray-50);
  }

  .agent-item:focus {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }

  .agent-item.status-error {
    border-left-color: var(--color-danger);
  }

  .agent-item.status-warning {
    border-left-color: var(--color-warning);
  }

  .agent-item.status-success {
    border-left-color: var(--color-success);
  }

  .agent-info {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
  }

  .agent-info i {
    font-size: 0.95rem;
    flex-shrink: 0;
  }

  .agent-item.status-error .agent-info i {
    color: var(--color-danger);
  }

  .agent-item.status-warning .agent-info i {
    color: var(--color-warning);
  }

  .agent-item.status-success .agent-info i {
    color: var(--color-success);
  }

  .agent-name {
    font-weight: 500;
    font-size: 0.875rem;
    color: var(--color-gray-900);
  }

  .agent-result {
    font-size: 0.85rem;
    font-weight: 600;
    color: var(--color-gray-600);
    white-space: nowrap;
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

    .check-info {
      width: 100%;
    }

    .card-controls {
      width: 100%;
      justify-content: space-between;
    }

    .stats-container {
      flex: 1;
      padding: 0.4rem 0.75rem;
      gap: var(--spacing-sm);
    }

    .stat-label {
      font-size: 0.65rem;
    }

    .stat-value {
      font-size: 1.1rem;
    }

    .stat-divider {
      height: 1.5rem;
    }
  }

  @media (max-width: 576px) {
    .quality-check-card .card-body {
      padding: 0.875rem var(--spacing-md);
    }

    .check-header {
      flex-direction: column;
      align-items: flex-start;
      gap: var(--spacing-xs);
    }

    .check-name {
      font-size: 0.9rem;
    }

    .check-description {
      font-size: 0.8rem;
    }

    .stats-container {
      gap: var(--spacing-sm);
      padding: 0.4rem 0.6rem;
    }

    .stat-label {
      font-size: 0.6rem;
    }

    .stat-value {
      font-size: 1rem;
    }

    .action-btn {
      width: 32px;
      height: 32px;
    }
  }
</style>
