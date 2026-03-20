<template>
  <div class="check-row card border-0 shadow-sm mb-2" @click="handleCardClick">
    <div class="card-body">
      <div class="check-row-content">
        <!-- Left: Check Information -->
        <div class="check-info">
          <div class="check-header">
            <i :class="getCheckStatusIcon()" class="check-icon"></i>
            <div class="check-text">
              <h6 class="check-name">{{ checkName }}</h6>
              <span v-if="category" class="check-category">{{ category }}</span>
            </div>
          </div>
        </div>

        <!-- Right: Patient Count -->
        <div class="check-metrics">
          <span class="metric-value">{{ formattedPatientCount }}/{{ formattedPercentage }}</span>
        </div>

        <!-- Status Badge -->
        <div v-if="checkStatus === CheckStatus.FAILED" class="check-status">
          <span class="status-badge" :class="getStatusBadgeClass()">
            {{ getStatusText() }}
          </span>
        </div>

        <!-- Expand Button -->
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

    <!-- Expanded Content - Per-Site Results -->
    <Transition name="expand">
      <div v-if="isExpanded" class="expanded-section">
        <div class="site-results-container">
          <div class="results-header">
            <h6>Results by Site</h6>
            <span class="text-muted small">{{ siteResultsCount }} site(s)</span>
          </div>

          <div class="site-list">
            <div
              v-for="siteResult in siteResults"
              :key="siteResult.agentId"
              class="site-item"
              :class="getSiteStatusClass(siteResult)"
              role="button"
              tabindex="0"
              :title="`View details for ${siteResult.agentName}`"
              @click.stop="navigateToAgent(siteResult.agentId)"
              @keydown.enter.stop="navigateToAgent(siteResult.agentId)"
              @keydown.space.prevent.stop="navigateToAgent(siteResult.agentId)"
            >
              <div class="site-item-info">
                <i class="bi bi-database-fill-gear site-icon"></i>
                <div class="site-item-text">
                  <span class="site-item-name">{{ siteResult.agentName }}</span>
                </div>
              </div>
              <span class="site-item-result"
                >{{ siteResult.formattedCount }}/{{ siteResult.formattedPercentage }}</span
              >
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
    checkHash: {
      type: String,
      required: true,
    },
    checkName: {
      type: String,
      required: true,
    },
    category: {
      type: String,
      default: null,
    },
    patientsMeetingCriteria: {
      type: Number,
      required: true,
    },
    totalPatients: {
      type: Number,
      required: true,
    },
    qualityCheck: {
      type: Object,
      required: true,
    },
    reports: {
      type: Array,
      required: true,
    },
    agents: {
      type: Array,
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

  const expandButtonTitle = computed(() => (isExpanded.value ? 'Collapse' : 'Expand'));

  const expandIconClass = computed(() =>
    isExpanded.value ? 'bi bi-chevron-up' : 'bi bi-chevron-down'
  );

  const handleCardClick = () => {
    isExpanded.value = !isExpanded.value;
  };

  /**
   * Get per-site results for this quality check
   */
  const siteResults = computed(() => {
    const results = [];

    // Get the most recent report per agent
    const reportsByAgent = new Map();
    props.reports.forEach((report) => {
      if (report.totalPatients != null && report.totalSamples != null) {
        const agentId = report.agentId;
        const existing = reportsByAgent.get(agentId);

        if (!existing || new Date(report.timestamp) > new Date(existing.timestamp)) {
          reportsByAgent.set(agentId, report);
        }
      }
    });

    // Process each report to find this check's result
    reportsByAgent.forEach((report, agentId) => {
      if (!report.results) return;

      // Filter by group if selected
      if (props.selectedGroup) {
        const agent = props.agents.find((a) => a.id === agentId);
        if (!agent || !agent.groups || !Array.isArray(agent.groups)) {
          return;
        }
        const hasGroup = agent.groups.some((group) => group && group.name === props.selectedGroup);
        if (!hasGroup) return;
      }

      const checkResult = report.results.find((r) => r.hash === props.checkHash);
      if (!checkResult) return;

      const agent = props.agents.find((a) => a.id === agentId);
      const agentName = agent ? agent.name : agentId;

      // Calculate patients meeting criteria from this site
      const resultPercentage =
        checkResult.result <= 1 ? checkResult.result : checkResult.result / 100;
      const patientsFromThisSite = Math.round(resultPercentage * report.totalPatients);
      const percentageFromThisSite = (patientsFromThisSite / report.totalPatients) * 100;

      // Determine status based on thresholds
      let status = CheckStatus.PASSED;
      if (percentageFromThisSite > props.qualityCheck.errorThreshold) {
        status = CheckStatus.FAILED;
      } else if (percentageFromThisSite > props.qualityCheck.warningThreshold) {
        status = CheckStatus.WARNING;
      }

      results.push({
        agentId: agentId,
        agentName: agentName,
        patientCount: patientsFromThisSite,
        totalPatients: report.totalPatients,
        percentage: percentageFromThisSite,
        formattedCount: patientsFromThisSite.toLocaleString(),
        formattedPercentage: `${percentageFromThisSite.toFixed(1)}%`,
        status: status,
      });
    });

    // Sort by patient count (highest first)
    return results.sort((a, b) => b.patientCount - a.patientCount);
  });

  /**
   * Navigate to agent report page
   */
  function navigateToAgent(agentId) {
    router.push({ name: 'AgentReport', params: { uuid: agentId } });
  }

  /**
   * Get status class for a site result
   */
  function getSiteStatusClass(siteResult) {
    return {
      'status-error': siteResult.status === CheckStatus.FAILED,
      'status-warning': siteResult.status === CheckStatus.WARNING,
      'status-success': siteResult.status === CheckStatus.PASSED,
    };
  }

  const siteResultsCount = computed(() => siteResults.value.length);

  /**
   * Calculate the overall percentage of patients meeting criteria
   */
  const overallPercentage = computed(() => {
    if (props.totalPatients === 0) return 0;
    return (props.patientsMeetingCriteria / props.totalPatients) * 100;
  });

  /**
   * Format patient count with thousands separator
   */
  const formattedPatientCount = computed(() => {
    return props.patientsMeetingCriteria.toLocaleString();
  });

  /**
   * Format percentage to 1 decimal place with % suffix
   */
  const formattedPercentage = computed(() => {
    return `${overallPercentage.value.toFixed(1)}%`;
  });

  /**
   * Determine the status based on the worst status among all site results
   */
  const checkStatus = computed(() => {
    // If no site results, use aggregated percentage as fallback
    if (siteResults.value.length === 0) {
      const percentage = overallPercentage.value;
      if (percentage > props.qualityCheck.errorThreshold) {
        return CheckStatus.FAILED;
      } else if (percentage > props.qualityCheck.warningThreshold) {
        return CheckStatus.WARNING;
      }
      return CheckStatus.PASSED;
    }

    // Find the worst status among all sites
    let worstStatus = CheckStatus.PASSED;

    for (const siteResult of siteResults.value) {
      if (siteResult.status === CheckStatus.FAILED) {
        return CheckStatus.FAILED; // Can't get worse than failed
      } else if (siteResult.status === CheckStatus.WARNING) {
        worstStatus = CheckStatus.WARNING;
      }
    }

    return worstStatus;
  });

  /**
   * Get the appropriate icon class based on status
   */
  function getCheckStatusIcon() {
    switch (checkStatus.value) {
      case CheckStatus.FAILED:
        return 'bi bi-exclamation-triangle-fill text-danger';
      case CheckStatus.WARNING:
        return 'bi bi-exclamation-circle-fill text-warning';
      case CheckStatus.PASSED:
      default:
        return 'bi bi-check-circle-fill text-success';
    }
  }

  /**
   * Get the status badge class
   */
  function getStatusBadgeClass() {
    switch (checkStatus.value) {
      case CheckStatus.FAILED:
        return 'badge-error';
      case CheckStatus.WARNING:
        return 'badge-warning';
      case CheckStatus.PASSED:
      default:
        return 'badge-success';
    }
  }

  /**
   * Get the status text
   */
  function getStatusText() {
    switch (checkStatus.value) {
      case CheckStatus.FAILED:
        return 'Failed';
      case CheckStatus.WARNING:
        return 'Warning';
      case CheckStatus.PASSED:
      default:
        return 'Passed';
    }
  }
</script>

<style scoped>
  /* Card Container */
  .check-row {
    transition:
      transform var(--transition-base),
      box-shadow var(--transition-base);
    cursor: pointer;
    margin-bottom: var(--spacing-sm);
    border-left: 4px solid transparent;
  }

  .check-row:hover {
    transform: translateX(2px);
    box-shadow: var(--shadow-md) !important;
  }

  .card-body {
    padding: var(--spacing-md) var(--spacing-lg);
  }

  /* Card Content Layout */
  .check-row-content {
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
  }

  .check-icon {
    font-size: 1.5rem;
    flex-shrink: 0;
  }

  .check-text {
    flex: 1;
    min-width: 0;
  }

  .check-name {
    font-weight: 600;
    font-size: 1.1rem;
    color: var(--color-gray-900);
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .check-category {
    display: inline-block;
    margin-top: 0.125rem;
    font-size: 0.75rem;
    color: var(--color-gray-500);
    font-style: italic;
  }

  /* Check Metrics */
  .check-metrics {
    display: flex;
    align-items: center;
    flex-shrink: 0;
  }

  .metric-value {
    font-size: 0.85rem;
    font-weight: 600;
    color: var(--color-gray-600);
    white-space: nowrap;
  }

  /* Status Badge */
  .check-status {
    flex-shrink: 0;
  }

  .status-badge {
    padding: 0.375rem 0.75rem;
    border-radius: var(--radius-md);
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .badge-warning {
    background: #fff3cd;
    color: #997404;
  }

  .badge-error {
    background: #f8d7da;
    color: #842029;
  }

  /* Action Buttons */
  .action-buttons {
    display: flex;
    gap: var(--spacing-xs);
    align-items: center;
    flex-shrink: 0;
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

  .site-results-container {
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

  /* Site List */
  .site-list {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-xs);
  }

  .site-item {
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

  .site-item:hover {
    transform: translateX(3px);
    box-shadow: var(--shadow-sm);
    background-color: var(--color-gray-50);
  }

  .site-item:focus {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }

  .site-item.status-error {
    border-left-color: var(--color-danger);
  }

  .site-item.status-warning {
    border-left-color: var(--color-warning);
  }

  .site-item.status-success {
    border-left-color: var(--color-success);
  }

  .site-item-info {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    flex: 1;
    min-width: 0;
  }

  .site-icon {
    font-size: 0.95rem;
    color: var(--color-primary);
    flex-shrink: 0;
  }

  .site-item-text {
    display: flex;
    flex-direction: column;
    gap: 0.125rem;
    min-width: 0;
  }

  .site-item-name {
    font-weight: 500;
    font-size: 0.875rem;
    color: var(--color-gray-900);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .site-item-result {
    font-size: 0.85rem;
    font-weight: 600;
    color: var(--color-gray-600);
    white-space: nowrap;
    margin-left: var(--spacing-sm);
  }

  /* Expand Transition */
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
    .check-row-content {
      flex-direction: column;
      align-items: flex-start;
      gap: var(--spacing-md);
    }

    .check-info {
      width: 100%;
    }

    .check-metrics,
    .check-status,
    .action-buttons {
      width: 100%;
    }
  }

  @media (max-width: 576px) {
    .card-body {
      padding: 0.875rem var(--spacing-md);
    }

    .check-name {
      font-size: 1rem;
    }

    .check-icon {
      font-size: 1.25rem;
    }

    .action-btn {
      width: 32px;
      height: 32px;
    }

    .site-item-name {
      font-size: 0.8rem;
    }

    .metric-value,
    .site-item-result {
      font-size: 0.8rem;
    }
  }
</style>
