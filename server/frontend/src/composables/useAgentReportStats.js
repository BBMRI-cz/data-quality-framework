import { computed } from 'vue';
import { countChecksByStatus } from '@/utils/qualityCheckUtils.js';

const AGENT_STATUS_META = {
  ACTIVE: {
    color: 'var(--color-success)',
    trendType: 'positive',
  },
  PENDING: {
    color: 'var(--color-warning)',
    trendType: 'negative',
  },
  DECLINED: {
    color: 'var(--color-danger)',
    trendType: 'negative',
  },
  INACTIVE: {
    color: 'var(--color-danger)',
    trendType: 'negative',
  },
};

const DEFAULT_AGENT_STATUS_META = {
  color: 'var(--color-gray-600)',
  trendType: 'neutral',
};

function formatRelativeTime(dateString) {
  if (!dateString) {
    return 'N/A';
  }

  const date = new Date(dateString);
  if (Number.isNaN(date.getTime())) {
    return 'N/A';
  }

  const now = new Date();
  const diffMs = now - date;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;

  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

function toLabel(status) {
  if (!status) {
    return 'Unknown';
  }

  return status
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

export function useAgentReportStats({ agent, reports, qualityChecks, totalReports }) {
  const qualityCheckMap = computed(() => {
    const map = new Map();

    qualityChecks.value.forEach((check) => {
      map.set(check.hash, check);
    });

    return map;
  });

  const agentName = computed(() => {
    return agent.value?.name || 'Unknown Agent';
  });

  const agentVersion = computed(() => {
    return agent.value?.version || 'N/A';
  });

  const agentStatusLabel = computed(() => {
    return toLabel(agent.value?.status);
  });

  const agentStatusMeta = computed(() => {
    return AGENT_STATUS_META[agent.value?.status] || DEFAULT_AGENT_STATUS_META;
  });

  const agentStatusColor = computed(() => {
    return agentStatusMeta.value.color;
  });

  const agentStatusTrendType = computed(() => {
    return agentStatusMeta.value.trendType;
  });

  const reportStats = computed(() => {
    const total = typeof totalReports?.value === 'number' ? totalReports.value : reports.value.length;

    if (!total) {
      return {
        total: 0,
        failed: 0,
        passed: 0,
        warnings: 0,
        lastReportTime: 'N/A',
      };
    }

    const latestReport = reports.value.reduce((latest, current) => {
      if (!latest) {
        return current;
      }

      return new Date(current.timestamp) > new Date(latest.timestamp) ? current : latest;
    }, null);

    const counts = countChecksByStatus(latestReport, qualityCheckMap.value);

    return {
      total,
      failed: counts.failed,
      passed: counts.passed,
      warnings: counts.warnings,
      lastReportTime: formatRelativeTime(latestReport.timestamp),
    };
  });

  return {
    qualityCheckMap,
    reportStats,
    agentName,
    agentVersion,
    agentStatusLabel,
    agentStatusColor,
    agentStatusTrendType,
  };
}
