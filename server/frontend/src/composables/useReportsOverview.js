import { computed, ref } from 'vue';
import { apiService } from '@/services/apiService.js';
import { getReportStatus, buildQualityCheckMap, CheckStatus } from '@/utils/qualityCheckUtils.js';

export function useReportsOverview() {
  const reports = ref([]);
  const qualityCheckMap = ref(new Map());
  const agents = ref([]);
  const loading = ref(true);
  const error = ref(null);

  const currentPage = ref(0);
  const pageSize = ref(10);
  const totalReports = ref(0);

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

  const fetchData = async () => {
    loading.value = true;
    error.value = null;

    try {
      const [checksData, agentsData] = await Promise.all([
        apiService.getQualityChecksDetailed(),
        apiService.getAgents(),
      ]);

      const checks = Array.isArray(checksData) ? checksData : [];

      agents.value = agentsData?._embedded?.agents || (Array.isArray(agentsData) ? agentsData : []);

      qualityCheckMap.value = buildQualityCheckMap(checks);

      await fetchReportsPage();
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
    fetchData();
  };

  return {
    reports,
    qualityCheckMap,
    agents,
    loading,
    error,
    reportStats,
    fetchData,
    currentPage,
    pageSize,
    totalReports,
    changePage,
    refreshPage,
  };
}
