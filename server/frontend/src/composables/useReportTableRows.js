import { computed } from 'vue';
import { countChecksByStatus, getReportStatus } from '@/utils/qualityCheckUtils.js';
import { formatDateShort, formatTime } from '@/utils/dateUtils.js';
import { useStatuses } from '@/composables/useStatuses.js';

const reportTableColumns = [
  { key: 'id', label: 'Report ID' },
  { key: 'agentName', label: 'Agent Name' },
  { key: 'timestampText', label: 'Timestamp' },
  { key: 'status', label: 'Status' },
  { key: 'totalChecks', label: 'Total Checks' },
  { key: 'warnings', label: 'Warnings' },
  { key: 'errors', label: 'Errors' },
];

export function useReportTableRows({ reports, qualityCheckMap, agents, selectedStatus = null }) {
  const { getStatusMeta } = useStatuses();

  const columns = reportTableColumns;

  const agentMap = computed(() => {
    return new Map((agents?.value || []).map((agent) => [agent.id, agent]));
  });

  const activeStatus = computed(() => selectedStatus?.value || null);

  const filteredReports = computed(() => {
    if (!activeStatus.value) {
      return reports.value;
    }

    return reports.value.filter((report) => {
      const reportStatus = getReportStatus(report, qualityCheckMap.value);
      return reportStatus === activeStatus.value;
    });
  });

  const tableRows = computed(() => {
    return filteredReports.value.map((report) => {
      const counts = countChecksByStatus(report, qualityCheckMap.value);
      const agentName = agentMap.value.get(report.agentId)?.name || 'Unknown Agent';
      const status = getReportStatus(report, qualityCheckMap.value);
      const statusMeta = getStatusMeta(status);

      return {
        ...report,
        agentName,
        timestampText: `${formatDateShort(report.timestamp)} ${formatTime(report.timestamp)}`,
        status,
        statusColor: statusMeta.color,
        statusIcon: statusMeta.icon,
        statusIconStyle: statusMeta.iconStyle,
        totalChecks: counts.total,
        warnings: counts.warnings,
        errors: counts.failed,
      };
    });
  });

  return {
    columns,
    filteredReports,
    tableRows,
  };
}
