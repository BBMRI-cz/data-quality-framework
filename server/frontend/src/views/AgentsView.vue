<template>
  <div class="container-fluid px-2 px-md-3 py-3 py-md-4">
    <div class="row">
      <div class="col-12">
        <!-- Page Header -->
        <PageHeader
          title="Agents Management"
          mobile-title="Agents"
          subtitle="Manage and monitor all connected agents in the network"
          icon="bi bi-database-fill-gear"
        >
          <template #actions>
            <button
              class="btn btn-outline-primary btn-sm"
              :disabled="loading"
              @click="refreshAgents"
            >
              <i class="bi bi-arrow-clockwise"></i>
              <span class="d-none d-md-inline ms-1">Refresh</span>
            </button>
          </template>
        </PageHeader>

        <!-- Stats Cards -->
        <div class="stats-grid mb-3 mb-md-4">
          <div class="stat-card">
            <div class="stat-number text-dark">{{ agentStats.total }}</div>
            <div class="stat-label">Total</div>
          </div>
          <div class="stat-card">
            <div class="stat-number text-success">{{ agentStats.active }}</div>
            <div class="stat-label">Active</div>
          </div>
          <div class="stat-card">
            <div class="stat-number text-warning">{{ agentStats.pending }}</div>
            <div class="stat-label">Pending</div>
          </div>
          <div class="stat-card">
            <div class="stat-number text-secondary">{{ agentStats.inactive }}</div>
            <div class="stat-label">Inactive</div>
          </div>
        </div>

        <!-- Filters -->
        <div class="filters-card mb-3 mb-md-4">
          <div class="filters-content">
            <div class="search-filter">
              <input
                v-model="searchQuery"
                type="text"
                class="form-control"
                placeholder="Search agents..."
              />
            </div>
            <div class="select-filters">
              <select v-model="statusFilter" class="form-select">
                <option value="">All Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="PENDING">Pending</option>
                <option value="INACTIVE">Inactive</option>
                <option value="ERROR">Error</option>
              </select>
              <select v-model="sortBy" class="form-select">
                <option value="name">Sort by Name</option>
                <option value="status">Sort by Status</option>
                <option value="id">Sort by ID</option>
              </select>
            </div>
            <div class="results-count">
              <span class="text-muted small">{{ filteredAgents.length }} agents</span>
            </div>
          </div>
        </div>

        <!-- Loading state -->
        <div v-if="loading" class="loading-state">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading agents...</span>
          </div>
        </div>

        <!-- Error state -->
        <div v-else-if="error" class="alert alert-danger" role="alert">
          <h6 class="alert-heading">Error Loading Agents</h6>
          <p class="mb-0">{{ error }}</p>
        </div>

        <!-- Agents List -->
        <div v-else-if="filteredAgents.length > 0" class="agents-list">
          <div
            v-for="agent in filteredAgents"
            :key="agent.id"
            class="agent-item"
            :class="{ pending: agent.status === 'PENDING' }"
          >
            <!-- Mobile Layout -->
            <div
              class="agent-content-mobile d-block d-md-none"
              @click="navigateToAgentReports(agent)"
            >
              <div class="agent-header">
                <div class="agent-avatar">
                  <div class="avatar-circle" :class="getAvatarClass(agent.status)">
                    <i class="bi bi-database-fill-gear"></i>
                  </div>
                </div>
                <div class="agent-info">
                  <div class="agent-name">{{ agent.name || 'Unknown' }}</div>
                  <div class="agent-id">
                    <code>{{ agent.id }}</code>
                  </div>
                </div>
                <div class="agent-status">
                  <span :class="getStatusClass(agent.status)" class="badge">
                    {{ agent.status }}
                  </span>
                </div>
              </div>

              <div v-if="agent.status !== 'ERROR'" class="agent-actions" @click.stop>
                <!-- Processing indicator -->
                <div v-if="processingAgent === agent.id" class="processing-indicator">
                  <div class="spinner-border spinner-border-sm text-primary me-2" role="status">
                    <span class="visually-hidden">Processing...</span>
                  </div>
                  <small class="text-muted">Processing...</small>
                </div>

                <!-- Pending actions -->
                <div v-else-if="agent.status === 'PENDING'" class="pending-actions">
                  <button
                    class="btn btn-success btn-sm"
                    title="Approve Agent"
                    @click="approveAgent(agent)"
                  >
                    <i class="bi bi-check-lg me-1"></i>
                    Approve
                  </button>
                  <button
                    class="btn btn-outline-danger btn-sm"
                    title="Decline Agent"
                    @click="declineAgent(agent)"
                  >
                    <i class="bi bi-x-lg me-1"></i>
                    Decline
                  </button>
                </div>

                <!-- Active/Inactive toggle -->
                <div
                  v-else-if="agent.status === 'ACTIVE' || agent.status === 'INACTIVE'"
                  class="toggle-actions"
                >
                  <div class="form-check form-switch">
                    <input
                      :id="`toggle-mobile-${agent.id}`"
                      class="form-check-input"
                      type="checkbox"
                      :checked="agent.status === 'ACTIVE'"
                      :disabled="processingAgent === agent.id"
                      @change="toggleAgentStatus(agent)"
                    />
                    <label class="form-check-label" :for="`toggle-mobile-${agent.id}`">
                      {{ agent.status === 'ACTIVE' ? 'Active' : 'Inactive' }}
                    </label>
                  </div>
                </div>
              </div>

              <!-- Pending message -->
              <div v-if="agent.status === 'PENDING'" class="pending-message">
                <div class="alert alert-warning">
                  <i class="bi bi-exclamation-triangle me-2"></i>
                  Awaiting approval
                </div>
              </div>
            </div>

            <!-- Desktop Layout -->
            <div
              class="agent-content-desktop d-none d-md-block"
              @click="navigateToAgentReports(agent)"
            >
              <div class="agent-row">
                <div class="agent-col-info">
                  <div class="d-flex align-items-center">
                    <div class="agent-avatar me-3">
                      <div class="avatar-circle" :class="getAvatarClass(agent.status)">
                        <i class="bi bi-database-fill-gear"></i>
                      </div>
                    </div>
                    <div class="agent-details">
                      <div class="agent-name">{{ agent.name || 'Unknown' }}</div>
                      <div class="agent-id">
                        <code>{{ agent.id }}</code>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="agent-col-status">
                  <span :class="getStatusClass(agent.status)" class="badge">
                    {{ agent.status }}
                  </span>
                </div>

                <div class="agent-col-actions" @click.stop>
                  <!-- Processing indicator -->
                  <div v-if="processingAgent === agent.id" class="d-flex align-items-center">
                    <div class="spinner-border spinner-border-sm text-primary me-2" role="status">
                      <span class="visually-hidden">Processing...</span>
                    </div>
                    <small class="text-muted">Processing...</small>
                  </div>

                  <!-- Pending actions -->
                  <div v-else-if="agent.status === 'PENDING'" class="d-flex gap-2">
                    <button
                      class="btn btn-success btn-sm"
                      title="Approve Agent"
                      @click="approveAgent(agent)"
                    >
                      <i class="bi bi-check-lg me-1"></i>
                      Approve
                    </button>
                    <button
                      class="btn btn-outline-danger btn-sm"
                      title="Decline Agent"
                      @click="declineAgent(agent)"
                    >
                      <i class="bi bi-x-lg me-1"></i>
                      Decline
                    </button>
                  </div>

                  <!-- Active/Inactive toggle -->
                  <div
                    v-else-if="agent.status === 'ACTIVE' || agent.status === 'INACTIVE'"
                    class="d-flex align-items-center"
                  >
                    <div class="form-check form-switch">
                      <input
                        :id="`toggle-desktop-${agent.id}`"
                        class="form-check-input"
                        type="checkbox"
                        :checked="agent.status === 'ACTIVE'"
                        :disabled="processingAgent === agent.id"
                        @change="toggleAgentStatus(agent)"
                      />
                      <label
                        class="form-check-label small text-muted ms-1"
                        :for="`toggle-desktop-${agent.id}`"
                      >
                        {{ agent.status === 'ACTIVE' ? 'Active' : 'Inactive' }}
                      </label>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Pending message for desktop -->
              <div v-if="agent.status === 'PENDING'" class="pending-message-desktop">
                <div class="alert alert-warning py-2 mb-0">
                  <small class="d-flex align-items-center">
                    <i class="bi bi-exclamation-triangle me-2 text-warning"></i>
                    This agent is awaiting approval to join the network
                  </small>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- No agents state -->
        <div v-else class="no-agents-state">
          <i class="bi bi-database-fill-gear mb-3 opacity-50"></i>
          <h4 class="text-muted">No agents found</h4>
          <p class="text-muted">
            {{
              searchQuery || statusFilter
                ? 'Try adjusting your filters'
                : 'No agents are currently registered in the system'
            }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { apiService } from '../services/apiService.js';
  import PageHeader from '../components/PageHeader.vue';

  const router = useRouter();
  const loading = ref(true);
  const error = ref(null);
  const agents = ref([]);
  const processingAgent = ref(null);

  // Filters
  const searchQuery = ref('');
  const statusFilter = ref('');
  const sortBy = ref('name');

  const fetchAgents = async () => {
    try {
      loading.value = true;
      error.value = null;
      const response = await apiService.getAgents();
      agents.value = response._embedded?.agents || [];
    } catch (err) {
      error.value = err.message || 'Failed to load agents';
      console.error('Error fetching agents:', err);
    } finally {
      loading.value = false;
    }
  };

  const refreshAgents = () => {
    fetchAgents();
  };

  // Computed properties
  const agentStats = computed(() => {
    const stats = {
      total: agents.value.length,
      active: 0,
      pending: 0,
      inactive: 0,
      error: 0,
    };

    agents.value.forEach((agent) => {
      switch (agent.status) {
        case 'ACTIVE':
          stats.active++;
          break;
        case 'PENDING':
          stats.pending++;
          break;
        case 'INACTIVE':
          stats.inactive++;
          break;
        case 'ERROR':
          stats.error++;
          break;
      }
    });

    return stats;
  });

  const filteredAgents = computed(() => {
    let filtered = agents.value;

    // Filter by search query
    if (searchQuery.value) {
      const query = searchQuery.value.toLowerCase();
      filtered = filtered.filter(
        (agent) =>
          (agent.name || 'unknown').toLowerCase().includes(query) ||
          agent.id.toLowerCase().includes(query)
      );
    }

    // Filter by status
    if (statusFilter.value) {
      filtered = filtered.filter((agent) => agent.status === statusFilter.value);
    }

    // Sort
    filtered.sort((a, b) => {
      switch (sortBy.value) {
        case 'name':
          return (a.name || 'Unknown').localeCompare(b.name || 'Unknown');
        case 'status':
          return a.status.localeCompare(b.status);
        case 'id':
          return a.id.localeCompare(b.id);
        default:
          return 0;
      }
    });

    return filtered;
  });

  // Methods
  const getStatusClass = (status) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-success';
      case 'PENDING':
        return 'bg-warning text-dark';
      case 'INACTIVE':
        return 'bg-secondary';
      case 'ERROR':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  };

  const getAvatarClass = (status) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-success text-white';
      case 'PENDING':
        return 'bg-warning text-dark';
      case 'INACTIVE':
        return 'bg-secondary text-white';
      case 'ERROR':
        return 'bg-danger text-white';
      default:
        return 'bg-secondary text-white';
    }
  };

  const approveAgent = async (agent) => {
    try {
      processingAgent.value = agent.id;
      await apiService.approveAgent(agent.id);
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

  const toggleAgentStatus = async (agent) => {
    const newStatus = agent.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      processingAgent.value = agent.id;
      await apiService.updateAgentStatus(agent.id, newStatus);
      const agentIndex = agents.value.findIndex((a) => a.id === agent.id);
      if (agentIndex !== -1) {
        agents.value[agentIndex].status = newStatus;
      }
    } catch (err) {
      console.error('Error updating agent status:', err);
      error.value = `Failed to update agent status: ${err.message}`;
    } finally {
      processingAgent.value = null;
    }
  };

  const navigateToAgentReports = (agent) => {
    router.push(`/agents/${agent.id}/reports`);
  };

  onMounted(() => {
    fetchAgents();
  });
</script>

<style scoped>
  /* Base styles */
  .page-title {
    font-size: 1.5rem;
    font-weight: 700;
    color: #212529;
    margin: 0;
  }

  .page-subtitle {
    color: #6c757d;
    font-size: 0.875rem;
    margin: 0;
  }

  .header-actions {
    flex-shrink: 0;
  }

  /* Stats Grid */
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 0.75rem;
  }

  .stat-card {
    background: #fff;
    border: 1px solid #dee2e6;
    border-radius: 0.5rem;
    padding: 1rem;
    text-align: center;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
    transition: all 0.2s ease;
  }

  .stat-card:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  }

  .stat-number {
    font-size: 1.25rem;
    font-weight: 700;
    margin-bottom: 0.25rem;
  }

  .stat-label {
    font-size: 0.75rem;
    color: #6c757d;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  /* Filters */
  .filters-card {
    background: #fff;
    border: 1px solid #dee2e6;
    border-radius: 0.5rem;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  }

  .filters-content {
    padding: 1rem;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .search-filter {
    width: 100%;
  }

  .select-filters {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.5rem;
  }

  .results-count {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    padding-top: 0.5rem;
    border-top: 1px solid #f8f9fa;
  }

  /* Loading and No Data States */
  .loading-state {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 3rem 0;
  }

  .no-agents-state {
    text-align: center;
    padding: 3rem 1rem;
  }

  .no-agents-state i {
    font-size: 3rem;
    color: #6c757d;
  }

  /* Agent Items */
  .agents-list {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .agent-item {
    background: #fff;
    border: 1px solid #dee2e6;
    border-radius: 0.5rem;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
    transition: all 0.2s ease;
    overflow: hidden;
  }

  .agent-item:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  .agent-item.pending {
    border-color: #ffc107;
    border-width: 2px;
  }

  /* Mobile Layout */
  .agent-content-mobile {
    padding: 1rem;
    cursor: pointer;
  }

  .agent-header {
    display: flex;
    align-items: flex-start;
    gap: 0.75rem;
    margin-bottom: 1rem;
  }

  .agent-avatar {
    flex-shrink: 0;
  }

  .avatar-circle {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1rem;
  }

  .agent-info {
    flex: 1;
    min-width: 0;
  }

  .agent-name {
    font-weight: 600;
    color: #212529;
    margin-bottom: 0.25rem;
    word-break: break-word;
  }

  .agent-id {
    font-size: 0.75rem;
  }

  .agent-id code {
    background: none;
    color: #6c757d;
    font-size: 0.75rem;
    padding: 0;
    word-break: break-all;
  }

  .agent-status {
    flex-shrink: 0;
  }

  .agent-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    justify-content: flex-start;
  }

  .processing-indicator {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    color: #6c757d;
  }

  .pending-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    width: 100%;
  }

  .pending-actions button {
    flex: 1;
    min-width: 120px;
  }

  .toggle-actions {
    display: flex;
    align-items: center;
  }

  .pending-message {
    margin-top: 1rem;
  }

  .pending-message .alert {
    margin: 0;
    padding: 0.5rem 0.75rem;
    font-size: 0.875rem;
  }

  /* Desktop Layout */
  .agent-content-desktop {
    padding: 1rem 1.5rem;
    cursor: pointer;
  }

  .agent-row {
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .agent-col-info {
    flex: 1;
    min-width: 0;
  }

  .agent-col-status {
    flex-shrink: 0;
  }

  .agent-col-actions {
    flex-shrink: 0;
    min-width: 200px;
    display: flex;
    justify-content: flex-end;
  }

  .pending-message-desktop {
    margin-top: 1rem;
    padding-top: 1rem;
    border-top: 1px solid #f8f9fa;
  }

  /* Badge styles */
  .badge {
    font-size: 0.75rem;
    padding: 0.375rem 0.75rem;
  }

  /* Form controls */
  .form-control-sm {
    font-size: 0.875rem;
  }

  .form-check-input {
    cursor: pointer;
  }

  .form-check-label {
    cursor: pointer;
    user-select: none;
  }

  /* Responsive breakpoints */
  @media (min-width: 576px) {
    .stats-grid {
      grid-template-columns: repeat(4, 1fr);
    }

    .page-title {
      font-size: 1.75rem;
    }

    .stat-number {
      font-size: 1.5rem;
    }
  }

  @media (min-width: 768px) {
    .filters-content {
      flex-direction: row;
      align-items: center;
    }

    .search-filter {
      flex: 2;
    }

    .select-filters {
      flex: 1;
      grid-template-columns: 1fr 1fr;
    }

    .results-count {
      flex-shrink: 0;
      justify-content: flex-end;
      padding-top: 0;
      border-top: none;
      margin-left: 1rem;
    }

    .page-title {
      font-size: 2rem;
    }
  }

  @media (min-width: 992px) {
    .agent-col-actions {
      min-width: 250px;
    }

    .filters-content {
      gap: 1rem;
    }

    .select-filters {
      grid-template-columns: 2fr 1fr;
    }
  }

  /* Animation for state changes */
  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  .agent-item {
    animation: fadeIn 0.3s ease-out;
  }

  /* Focus states for accessibility */
  .form-control:focus,
  .form-select:focus {
    border-color: #86b7fe;
    box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.25);
  }

  .btn:focus {
    box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.25);
  }

  /* High contrast mode support */
  @media (prefers-contrast) {
    .agent-item {
      border-width: 2px;
    }

    .stat-card {
      border-width: 2px;
    }

    .filters-card {
      border-width: 2px;
    }
  }

  /* Reduced motion support */
  @media (prefers-reduced-motion) {
    .agent-item {
      animation: none !important;
      transition: none !important;
    }

    .stat-card {
      transition: none !important;
    }

    .agent-item:hover {
      transform: none !important;
    }

    .stat-card:hover {
      transform: none !important;
    }
  }
</style>
