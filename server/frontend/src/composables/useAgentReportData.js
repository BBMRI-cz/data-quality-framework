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

  const fetchAgentDetails = async () => {
    try {
      loading.value = true;
      error.value = null;

      const [qualityChecksResponse, agentsResponse] = await Promise.all([
        apiService.getQualityChecks(),
        apiService.getAgents(),
      ]);

      qualityChecks.value = qualityChecksResponse?._embedded?.qualityChecks || [];

      const agents = agentsResponse?._embedded?.agents || [];
      agent.value = agents.find((item) => item.id === agentId.value);

      if (!agent.value) {
        reports.value = [];
        totalReports.value = 0;
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
    fetchAgentDetails,
    changePage,
    refreshPage,
  };
}
