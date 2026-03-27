import { computed, ref } from 'vue';
import { apiService } from '@/services/apiService.js';
import { getReportStatus, CheckStatus } from '@/utils/qualityCheckUtils.js';
import { useStatuses } from '@/composables/useStatuses.js';

export function useReportsOverview() {
  const { allowedValues, statusOptions } = useStatuses();

  const reports = ref([]);
  const qualityCheckMap = ref(new Map());
  const agents = ref([]);
  const loading = ref(true);
  const error = ref(null);

  const selectedStatus = ref(null);
  const statuses = allowedValues;

  const reportStats = computed(() => {
    const stats = {
      passed: 0,
      warnings: 0,
      failed: 0,
    };

    reports.value.forEach((report) => {
      const status = getReportStatus(report, qualityCheckMap.value);

      switch (status) {
        case CheckStatus.PASSED:
          stats.passed += 1;
          break;
        case CheckStatus.WARNING:
          stats.warnings += 1;
          break;
        case CheckStatus.FAILED:
          stats.failed += 1;
          break;
      }
    });

    return stats;
  });

  const fetchData = async () => {
    loading.value = true;
    error.value = null;

    try {
      const [checksData, reportsData, agentsData] = await Promise.all([
        apiService.getQualityChecks(),
        apiService.getReports(),
        apiService.getAgents(),
      ]);

      const checks =
        checksData?._embedded?.qualityChecks || (Array.isArray(checksData) ? checksData : []);

      const reportsArray =
        reportsData?._embedded?.reports || (Array.isArray(reportsData) ? reportsData : []);

      agents.value = agentsData?._embedded?.agents || (Array.isArray(agentsData) ? agentsData : []);

      qualityCheckMap.value = new Map(checks.map((check) => [check.hash, check]));

      reports.value = reportsArray
        .slice()
        .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
    } catch (err) {
      console.error('Error fetching reports:', err);
      error.value = err.message || 'Failed to load reports';
    } finally {
      loading.value = false;
    }
  };

  return {
    reports,
    qualityCheckMap,
    agents,
    loading,
    error,
    selectedStatus,
    statuses,
    statusOptions,
    reportStats,
    fetchData,
  };
}
