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

  const currentPage = ref(0);
  const pageSize = ref(10);
  const totalReports = ref(0);
  const statsReports = ref([]);
  const statsLoaded = ref(false);

  const selectedStatus = ref(null);
  const statuses = allowedValues;

  const reportStats = computed(() => {
    const stats = {
      passed: 0,
      warnings: 0,
      failed: 0,
    };

    statsReports.value.forEach((report) => {
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

  const parseReportsResponse = (reportsData) => {
    const reportsArray =
      reportsData?._embedded?.reports || (Array.isArray(reportsData) ? reportsData : []);
    const pageInfo = reportsData?.page || null;
    return { reportsArray, pageInfo };
  };

  const fetchReportsPage = async () => {
    const reportsData = await apiService.getReports({
      page: currentPage.value,
      size: pageSize.value,
    });
    const { reportsArray, pageInfo } = parseReportsResponse(reportsData);

    reports.value = reportsArray;
    totalReports.value = pageInfo?.totalElements ?? reportsArray.length;
  };

  const fetchReportStats = async () => {
    if (!totalReports.value) {
      statsReports.value = [];
      statsLoaded.value = true;
      return;
    }

    if (totalReports.value <= pageSize.value) {
      statsReports.value = reports.value;
      statsLoaded.value = true;
      return;
    }

    const statsPageSize = 200;
    const totalPages = Math.ceil(totalReports.value / statsPageSize);
    const allReports = [];

    for (let page = 0; page < totalPages; page += 1) {
      const response = await apiService.getReports({ page, size: statsPageSize });
      const { reportsArray } = parseReportsResponse(response);
      allReports.push(...reportsArray);
    }

    statsReports.value = allReports;
    statsLoaded.value = true;
  };

  const fetchData = async () => {
    loading.value = true;
    error.value = null;

    try {
      const [checksData, agentsData] = await Promise.all([
        apiService.getQualityChecks(),
        apiService.getAgents(),
      ]);

      const checks =
        checksData?._embedded?.qualityChecks || (Array.isArray(checksData) ? checksData : []);

      agents.value = agentsData?._embedded?.agents || (Array.isArray(agentsData) ? agentsData : []);

      qualityCheckMap.value = new Map(checks.map((check) => [check.hash, check]));

      await fetchReportsPage();

      if (!statsLoaded.value) {
        await fetchReportStats();
      }
    } catch (err) {
      console.error('Error fetching reports:', err);
      error.value = err.message || 'Failed to load reports';
    } finally {
      loading.value = false;
    }
  };

  const changePage = (nextPage) => {
    currentPage.value = nextPage;
    fetchData();
  };

  const refreshPage = () => {
    currentPage.value = 0;
    statsLoaded.value = false;
    fetchData();
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
    currentPage,
    pageSize,
    totalReports,
    changePage,
    refreshPage,
  };
}
