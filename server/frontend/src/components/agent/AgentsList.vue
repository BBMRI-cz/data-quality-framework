<template>
  <div class="agents-container">
    <div class="card border-0 shadow-md rounded-3 overflow-hidden h-100">
      <div class="card-header px-4 py-3 border-bottom-0 header-gradient">
        <h6 class="mb-0 fw-semibold text-white">
          <i class="bi bi-database-fill-gear me-2"></i>
          Agents
        </h6>
      </div>
      <div class="card-body p-0">
        <!-- Loading state -->
        <div
          v-if="loading"
          class="d-flex justify-content-center align-items-center"
          style="min-height: 80px"
        >
          <div class="spinner-border spinner-border-sm text-primary" role="status">
            <span class="visually-hidden">Loading agents...</span>
          </div>
        </div>

        <!-- Error state -->
        <div v-else-if="error" class="alert alert-danger mb-0 mx-4 my-2" role="alert">
          <small>{{ error }}</small>
        </div>

        <!-- Agents list -->
        <div v-else-if="agents.length > 0" class="agents-list">
          <div
            v-for="agent in agents"
            :key="agent.id"
            class="agent-item border-bottom px-4 py-3 hover-bg-light cursor-pointer"
            :class="{ 'pending-agent': agent.status === 'PENDING' }"
            @click="navigateToAgentReport(agent)"
          >
            <div class="d-flex align-items-center justify-content-between">
              <div class="d-flex align-items-center flex-grow-1">
                <div
                  class="status-indicator rounded-circle me-3"
                  :class="getStatusIndicatorClass(agent.status)"
                  style="min-width: 12px; width: 12px; height: 12px"
                ></div>
                <div class="flex-grow-1">
                  <div class="agent-name fw-medium text-dark">
                    {{ agent.name || 'Unknown' }}
                  </div>
                  <p class="agent-id text-muted small mb-0">ID: {{ truncateId(agent.id) }}</p>
                </div>
              </div>
              <div class="agent-actions d-flex align-items-center gap-2 ms-2">
                <!-- Display status badge only -->
                <Badge
                  :text="getStatusDisplayText(agent.status)"
                  :color="getStatusColor(agent.status)"
                  size="small"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- No agents state -->
        <div v-else class="text-center text-muted py-4 px-4">
          <i class="bi bi-database-fill-gear mb-2 opacity-50" style="font-size: 1.5rem"></i>
          <p class="mb-0 small">No agents available</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, onMounted, computed, watch } from 'vue';
  import { useRouter } from 'vue-router';
  import { apiService } from '@/services/apiService.js';
  import Badge from '@/components/ui/Badge.vue';

  const router = useRouter();
  const emit = defineEmits(['agentsLoaded']);

  const loading = ref(true);
  const error = ref(null);
  const agents = ref([]);
  const processingAgent = ref(null);

  const activeAgentsCount = computed(() => {
    return agents.value.filter((agent) => agent.status === 'ACTIVE').length;
  });

  // Emit active agents count whenever it changes
  watch(
    activeAgentsCount,
    (count) => {
      emit('agentsLoaded', count);
    },
    { immediate: true }
  );

  const fetchAgents = async () => {
    try {
      loading.value = true;
      error.value = null;
      const response = await apiService.getAgents();
      const allAgents = response._embedded?.agents || [];
      // Sort agents so PENDING status appears first
      agents.value = allAgents.sort((a, b) => {
        if (a.status === 'PENDING' && b.status !== 'PENDING') return -1;
        if (a.status !== 'PENDING' && b.status === 'PENDING') return 1;
        return 0;
      });
    } catch (err) {
      error.value = err.message || 'Failed to load agents';
      console.error('Error fetching agents:', err);
    } finally {
      loading.value = false;
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'ACTIVE':
        return '#198754';
      case 'INACTIVE':
        return '#6c757d';
      case 'ERROR':
        return '#dc3545';
      case 'PENDING':
        return '#ffc107';
      default:
        return '#6c757d';
    }
  };

  const getStatusIndicatorClass = (status) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-success';
      case 'INACTIVE':
        return 'bg-secondary';
      case 'ERROR':
        return 'bg-danger';
      case 'PENDING':
        return 'bg-warning';
      default:
        return 'bg-secondary';
    }
  };

  const getStatusDisplayText = (status) => {
    if (status === 'PENDING') {
      return 'Requires Attention';
    }
    return status;
  };

  const approveAgent = async (agent) => {
    try {
      processingAgent.value = agent.id;
      await apiService.approveAgent(agent.id);
      // Update the agent status locally
      const agentIndex = agents.value.findIndex((a) => a.id === agent.id);
      if (agentIndex !== -1) {
        agents.value[agentIndex].status = 'ACTIVE';
      }
    } catch (err) {
      console.error('Error approving agent:', err);
      error.value = `Failed to approve agent: ${err.message}`;
    } finally {
      processingAgent.value = null;
    }
  };

  const declineAgent = async (agent) => {
    try {
      processingAgent.value = agent.id;
      await apiService.declineAgent(agent.id);
      // Update the agent status locally
      const agentIndex = agents.value.findIndex((a) => a.id === agent.id);
      if (agentIndex !== -1) {
        agents.value[agentIndex].status = 'INACTIVE';
      }
    } catch (err) {
      console.error('Error declining agent:', err);
      error.value = `Failed to decline agent: ${err.message}`;
    } finally {
      processingAgent.value = null;
    }
  };

  const navigateToAgentReport = (agent) => {
    router.push(`/agents/${agent.id}/reports`);
  };

  const truncateId = (id) => {
    // Truncate the ID to a maximum of 8 characters for display
    return id.length > 8 ? id.slice(0, 8) + '...' : id;
  };

  onMounted(() => {
    fetchAgents();
  });
</script>

<style scoped>
  .agents-container {
    min-height: 200px;
  }

  .agent-item {
    transition: background-color 0.2s ease;
    border-left: none !important;
    border-right: none !important;
    border-top: none !important;
    position: relative;
  }

  .agent-item:last-child {
    border-bottom: none !important;
  }

  .hover-bg-light:hover {
    background-color: #f8f9fa !important;
  }

  .cursor-pointer {
    cursor: pointer;
  }

  .agent-name {
    font-size: 1rem;
    line-height: 1.3;
  }

  .agent-id {
    font-size: 0.875rem;
    line-height: 1.2;
  }

  .pending-agent {
    background-color: #fff3cd;
  }

  .status-indicator {
    flex-shrink: 0;
  }

  /* Header gradient to match sidebar */
  .header-gradient {
    background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  }

  @media (max-width: 768px) {
    .agent-item {
      flex-direction: column;
      align-items: flex-start;
      gap: 0.5rem;
    }

    .agent-actions {
      align-self: flex-end;
    }
  }
</style>
