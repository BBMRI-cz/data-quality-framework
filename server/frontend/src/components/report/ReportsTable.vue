<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white border-bottom py-3">
      <div class="d-flex justify-content-between align-items-center">
        <h5 class="mb-0 fw-semibold">Recent Reports</h5>
        <Badge :text="`${reports.length} reports`" variant="secondary" size="small" />
      </div>
    </div>
    <div class="card-body p-0">
      <div class="table-responsive">
        <table class="table table-hover mb-0 align-middle">
          <thead class="table-light">
            <tr>
              <th class="ps-4">Report ID</th>
              <th>Agent Name</th>
              <th>Timestamp</th>
              <th class="text-center">Status</th>
              <th class="text-center">Total Checks</th>
              <th class="text-center">Warnings</th>
              <th class="text-center">Errors</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="report in filteredReports"
              :key="report.id"
              class="cursor-pointer table-row-hover"
              @click="navigateToReport(report.id)"
            >
              <td class="ps-4">
                <div class="d-flex align-items-center">
                  <i class="bi bi-file-earmark-text text-primary me-2"></i>
                  <span class="font-monospace small text-truncate report-id">{{ report.id }}</span>
                </div>
              </td>
              <td>
                <div class="d-flex align-items-center">
                  <i class="bi bi-database-fill-gear text-info me-2"></i>
                  <span class="fw-medium">{{ getAgentName(report.agentId) }}</span>
                </div>
              </td>
              <td>
                <div class="d-flex flex-column">
                  <span class="fw-medium">{{ formatDateShort(report.timestamp) }}</span>
                  <small class="text-muted">{{ formatTime(report.timestamp) }}</small>
                </div>
              </td>
              <td class="text-center">
                <Badge
                  :text="getReportStatusText(report)"
                  :color="getReportStatusColor(report)"
                  size="small"
                  :class="{ 'status-alert': isReportStatusAlert(report) }"
                />
              </td>
              <td class="text-center">
                <span class="text-muted">{{ getCheckCounts(report).total }}</span>
              </td>
              <td class="text-center">
                <span
                  :class="
                    getCheckCounts(report).warnings > 0 ? 'text-warning fw-semibold' : 'text-muted'
                  "
                >
                  {{ getCheckCounts(report).warnings }}
                </span>
              </td>
              <td class="text-center">
                <span
                  :class="
                    getCheckCounts(report).failed > 0 ? 'text-danger fw-semibold' : 'text-muted'
                  "
                >
                  {{ getCheckCounts(report).failed }}
                </span>
              </td>
            </tr>
            <tr v-if="reports.length === 0">
              <td colspan="7" class="text-center text-muted py-5">
                <i class="bi bi-inbox fs-1 d-block mb-2 opacity-50"></i>
                <p class="mb-0">No reports available</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { computed } from 'vue';
  import { useRouter } from 'vue-router';
  import { countChecksByStatus, getReportStatus, CheckStatus } from '@/utils/qualityCheckUtils.js';
  import { formatDateShort, formatTime } from '@/utils/dateUtils.js';
  import Badge from '@/components/ui/Badge.vue';

  const router = useRouter();

  const props = defineProps({
    reports: {
      type: Array,
      required: true,
      default: () => [],
    },
    qualityCheckMap: {
      type: Map,
      required: true,
    },
    agents: {
      type: Array,
      required: true,
      default: () => [],
    },
    selectedStatus: {
      type: String,
      default: null,
    },
  });

  const filteredReports = computed(() => {
    if (!props.selectedStatus) {
      return props.reports;
    }

    return props.reports.filter((report) => {
      const reportStatus = getReportStatus(report, props.qualityCheckMap);
      return reportStatus === props.selectedStatus;
    });
  });

  // Create a map for quick agent lookup by ID
  const agentMap = computed(() => {
    return new Map(props.agents.map((agent) => [agent.id, agent]));
  });

  const getAgentName = (agentId) => {
    const agent = agentMap.value.get(agentId);
    return agent?.name || 'Unknown Agent';
  };

  const navigateToReport = (reportId) => {
    router.push(`/reports/${reportId}`);
  };

  const getCheckCounts = (report) => {
    return countChecksByStatus(report, props.qualityCheckMap);
  };

  const getReportStatusText = (report) => {
    return getReportStatus(report, props.qualityCheckMap);
  };

  const getReportStatusColor = (report) => {
    const status = getReportStatus(report, props.qualityCheckMap);

    switch (status) {
      case CheckStatus.PASSED:
        return '#198754';
      case CheckStatus.WARNING:
        return '#ffc107';
      case CheckStatus.FAILED:
        return '#dc3545';
      default:
        return '#6c757d';
    }
  };

  const isReportStatusAlert = (report) => {
    const status = getReportStatus(report, props.qualityCheckMap);
    return status === CheckStatus.WARNING || status === CheckStatus.FAILED;
  };
</script>

<style scoped>
  .table th {
    font-weight: 600;
    font-size: 0.813rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: #6c757d;
    padding: 1rem 0.75rem;
    border-bottom: 2px solid #dee2e6;
    white-space: nowrap;
  }

  .table td {
    vertical-align: middle;
    padding: 1rem 0.75rem;
    border-bottom: 1px solid #f0f0f0;
  }

  .table-responsive {
    overflow-x: visible;
  }

  .report-id {
    max-width: 150px;
    display: inline-block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .table-row-hover {
    transition: all 0.2s ease-in-out;
  }

  .table-row-hover:hover {
    background-color: #f8f9fa;
    transform: translateX(2px);
    box-shadow: inset 3px 0 0 #0d6efd;
  }

  .font-monospace {
    font-family: var(--font-mono), monospace;
    font-size: 0.875rem;
  }

  .cursor-pointer {
    cursor: pointer;
  }

  /* Status indicator animation */
  .status-alert {
    animation: pulse-subtle 2s ease-in-out infinite;
  }

  @keyframes pulse-subtle {
    0%,
    100% {
      opacity: 1;
    }
    50% {
      opacity: 0.85;
    }
  }
</style>
