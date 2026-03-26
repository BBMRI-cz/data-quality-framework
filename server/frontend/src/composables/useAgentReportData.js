import { ref } from 'vue';
import { apiService } from '@/services/apiService.js';

export function useAgentReportData(agentId) {
  const loading = ref(true);
  const error = ref(null);
  const agent = ref(null);
  const reports = ref([]);
  const qualityChecks = ref([]);

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
        error.value = 'Agent not found';
        return;
      }

      const reportsResponse = await apiService.getAgentReports(agentId.value);
      const reportsList = reportsResponse?._embedded?.reports || reportsResponse?.reports || [];

      reports.value = reportsList
        .slice()
        .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
    } catch (err) {
      error.value = err.message || 'Failed to load agent report';
      console.error('Error fetching agent details:', err);
    } finally {
      loading.value = false;
    }
  };

  return {
    loading,
    error,
    agent,
    reports,
    qualityChecks,
    fetchAgentDetails,
  };
}

