<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white border-bottom py-3">
      <div class="d-flex justify-content-between align-items-center">
        <h5 class="mb-0 fw-semibold">All Reports</h5>
        <span class="badge bg-secondary">{{ reports.length }} reports</span>
      </div>
    </div>
    <div class="card-body p-0">
      <div class="table-responsive">
        <table class="table table-hover mb-0 align-middle">
          <thead class="table-light">
            <tr>
              <th class="ps-4">Report ID</th>
              <th>Generated At</th>
              <th class="text-center">Status</th>
              <th class="text-center">Total Entities</th>
              <th class="text-center">Epsilon Budget</th>
              <th class="text-center">Epsilon Used</th>
              <th class="text-center">Total Checks</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="report in reports"
              :key="report.id"
              class="table-row-hover clickable"
              @click="navigateToReport(report.id)"
            >
              <td class="ps-4">
                <div class="d-flex align-items-center">
                  <i class="bi bi-file-earmark-text text-primary me-2"></i>
                  <span class="font-monospace small">{{ report.id }}</span>
                </div>
              </td>
              <td>
                <div class="d-flex flex-column">
                  <span class="fw-medium">{{ formatDateShort(report.generatedAt) }}</span>
                  <small class="text-muted">{{ formatTime(report.generatedAt) }}</small>
                </div>
              </td>
              <td class="text-center">
                <span class="badge rounded-pill" :class="getStatusBadgeClass(report)">
                  {{ report.status }}
                </span>
              </td>
              <td class="text-center">
                <span class="text-muted">{{ report.numberOfEntities || 'N/A' }}</span>
              </td>
              <td class="text-center">
                <span class="text-muted">{{
                  report.epsilonBudget ? report.epsilonBudget.toFixed(2) : 'N/A'
                }}</span>
              </td>
              <td class="text-center">
                <span :class="getEpsilonClass(report)">
                  {{ calculateEpsilonUsed(report).toFixed(2) }}
                </span>
              </td>
              <td class="text-center">
                <span class="text-muted">{{ report.results?.length || 0 }}</span>
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
  import { useRouter } from 'vue-router';

  const router = useRouter();

defineProps({
    reports: {
      type: Array,
      required: false,
      default: () => [],
    },
  });

  const navigateToReport = (reportId) => {
    router.push(`/reports/${reportId}`);
  };

  const formatDateShort = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  const formatTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const calculateEpsilonUsed = (report) => {
    if (!report.results || !Array.isArray(report.results)) return 0;
    return report.results.reduce((sum, result) => sum + (result.epsilon || 0), 0);
  };

  const getEpsilonClass = (report) => {
    const used = calculateEpsilonUsed(report);
    const budget = report.epsilonBudget || 0;

    if (used > budget) {
      return 'text-danger fw-semibold';
    }
    return 'text-muted';
  };

  const getStatusBadgeClass = (report) => {
    switch (report.status) {
      case 'COMPLETED':
        return 'bg-success';
      case 'GENERATING':
        return 'bg-warning text-dark';
      case 'FAILED':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  };
</script>

<style scoped>
  .table th {
    font-weight: 600;
    font-size: 0.813rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--color-gray-500);
    padding: var(--spacing-md) 0.75rem;
    border-bottom: 2px solid var(--color-gray-200);
    white-space: nowrap;
  }

  .table td {
    vertical-align: middle;
    padding: var(--spacing-md) 0.75rem;
    border-bottom: 1px solid #f0f0f0;
  }

  .table-responsive {
    overflow-x: visible;
  }

  .table-row-hover {
    cursor: pointer;
    transition: background-color var(--transition-base);
  }

  .table-row-hover:hover {
    background-color: var(--bg-hover);
  }

  .clickable {
    cursor: pointer;
  }

  .font-monospace {
    font-family: var(--font-mono);
    font-size: 0.875rem;
  }

  .badge {
    font-weight: 500;
    padding: 0.35rem 0.65rem;
    font-size: 0.75rem;
    white-space: nowrap;
  }

  .badge.rounded-pill {
    padding: 0.35rem 0.85rem;
  }

  .badge.bg-danger,
  .badge.bg-warning {
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

  @media (max-width: 992px) {
    .table th,
    .table td {
      padding: 0.75rem var(--spacing-sm);
    }
  }

  @media (max-width: 768px) {
    .table {
      font-size: 0.75rem;
    }

    .table th,
    .table td {
      padding: var(--spacing-sm) 0.35rem;
    }

    .badge {
      font-size: 0.65rem;
      padding: var(--spacing-xs) 0.45rem;
    }

    .badge i {
      display: none;
    }
  }

  @media (max-width: 576px) {
    .table-responsive {
      overflow-x: auto;
    }

    .table th:nth-child(4),
    .table td:nth-child(4),
    .table th:nth-child(5),
    .table td:nth-child(5),
    .table th:nth-child(6),
    .table td:nth-child(6) {
      display: none;
    }
  }
</style>
