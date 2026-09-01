import { ref } from 'vue';
import { apiService } from '@/services/apiService.js';

export function useAgentReportData(agentId) {
  const loading = ref(true);
  const error = ref(null);
  const agent = ref(null);
  const reports = ref([]);
  const qualityChecks = ref([]);
  const currentPage = ref(0);
  const pageSize = ref(5);
  const totalReports = ref(0);
  const latestReport = ref(null);

  const fetchAgentDetails = async () => {
    try {
      loading.value = true;
      error.value = null;

      const [qualityChecksResponse, agentsResponse] = await Promise.all([
        apiService.getQualityChecksDetailed(),
        apiService.getAgents(),
      ]);

      qualityChecks.value = Array.isArray(qualityChecksResponse)
        ? qualityChecksResponse
        : qualityChecksResponse?._embedded?.qualityChecks || [];

      const agents = agentsResponse?._embedded?.agents || [];
      agent.value = agents.find((item) => item.id === agentId.value);

      if (!agent.value) {
        reports.value = [];
        totalReports.value = 0;
        latestReport.value = null;
        error.value = 'Agent not found';
        return;
      }

      const reportsResponse = await apiService.getAgentReports(agentId.value, {
        page: currentPage.value,
        size: pageSize.value,
      });
      const reportsList =
        reportsResponse?._embedded?.reports ||
        reportsResponse?.reports ||
        (Array.isArray(reportsResponse) ? reportsResponse : []);
      const pageInfo = reportsResponse?.page || null;

      reports.value = reportsList;
      totalReports.value = pageInfo?.totalElements ?? reportsList.length;

      const latestSource =
        currentPage.value === 0
          ? reportsResponse
          : await apiService.getAgentReports(agentId.value, {
              page: 0,
              size: 1,
            });
      const latestList =
        latestSource?._embedded?.reports ||
        latestSource?.reports ||
        (Array.isArray(latestSource) ? latestSource : []);
      latestReport.value = latestList[0] || null;
    } catch (err) {
      error.value = err.message || 'Failed to load agent report';
      console.error('Error fetching agent details:', err);
    } finally {
      loading.value = false;
    }
  };

  const changePage = (nextPage) => {
    currentPage.value = nextPage;
    fetchAgentDetails();
  };

  const refreshPage = () => {
    currentPage.value = 0;
    fetchAgentDetails();
  };

  return {
    loading,
    error,
    agent,
    reports,
    qualityChecks,
    currentPage,
    pageSize,
    totalReports,
    latestReport,
    fetchAgentDetails,
    changePage,
    refreshPage,
  };
}
