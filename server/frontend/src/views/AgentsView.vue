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

        <!-- Empty state -->
        <div v-else-if="filteredAgents.length === 0" class="empty-state">
          <div class="empty-state-icon">
            <i class="bi bi-database-fill-gear"></i>
          </div>
          <h5 class="empty-state-title">No Agents Found</h5>
          <p class="empty-state-text">
            {{
              searchQuery || statusFilter
                ? 'Try adjusting your filters'
                : 'No agents are currently registered in the system'
            }}
          </p>
        </div>

        <!-- Agents Table -->
        <div v-else class="card border-0 shadow-sm">
          <div class="card-header bg-white border-bottom py-3">
            <div class="d-flex justify-content-between align-items-center">
              <h5 class="mb-0 fw-semibold">Agents</h5>
              <span class="badge bg-secondary">{{ filteredAgents.length }} agents</span>
            </div>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-hover mb-0 align-middle">
                <thead class="table-light">
                  <tr>
                    <th class="ps-4">ID</th>
                    <th>Name</th>
                    <th>External Identifier</th>
                    <th class="text-center">Status</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="agent in filteredAgents"
                    :key="agent.id"
                    class="table-row-hover cursor-pointer"
                    @click="navigateToAgentReports(agent)"
                  >
                    <td class="ps-4">
                      <code class="font-monospace small text-muted agent-id">{{ agent.id }}</code>
                    </td>
                    <td>
                      <span class="fw-medium">{{ agent.name || 'Unknown' }}</span>
                    </td>
                    <td>
                      <code v-if="agent.externalIdentifier" class="small text-muted">{{
                        agent.externalIdentifier
                      }}</code>
                      <span v-else class="text-muted small">None</span>
                    </td>
                    <td class="text-center">
                      <span :class="getStatusClass(agent.status)" class="badge rounded-pill">
                        {{ agent.status }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
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

  const navigateToAgentReports = (agent) => {
    router.push(`/agents/${agent.id}/reports`);
  };

  onMounted(() => {
    fetchAgents();
  });
</script>

<style scoped>
  /* Stats Grid */
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: 1rem;
  }

  .stat-card {
    background: white;
    border-radius: 8px;
    padding: 1.25rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    transition:
      transform 0.2s,
      box-shadow 0.2s;
  }

  .stat-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  }

  .stat-number {
    font-size: 1.75rem;
    font-weight: 700;
    line-height: 1.2;
    margin-bottom: 0.25rem;
  }

  .stat-label {
    font-size: 0.813rem;
    color: #6c757d;
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  /* Filters */
  .filters-card {
    background: white;
    padding: 1rem;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  .filters-content {
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
    justify-content: space-between;
    align-items: center;
    padding-top: 0.5rem;
    border-top: 1px solid #f8f9fa;
  }

  /* Loading State */
  .loading-state {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 300px;
  }

  .spinner-border {
    width: 3rem;
    height: 3rem;
  }

  /* Empty State */
  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  .empty-state-icon {
    font-size: 4rem;
    color: #e0e0e0;
    margin-bottom: 1rem;
  }

  .empty-state-title {
    color: #2c3e50;
    font-weight: 600;
    margin-bottom: 0.5rem;
  }

  .empty-state-text {
    color: #6c757d;
    margin-bottom: 0;
  }

  /* Table Styling */
  .card {
    border-radius: 12px;
    overflow: hidden;
  }

  .table {
    font-size: 0.875rem;
  }

  .table th {
    font-weight: 600;
    font-size: 0.813rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: #6c757d;
    padding: 1rem 0.75rem;
    border-bottom: 2px solid #dee2e6;
    white-space: nowrap;
  }

  .table td {
    vertical-align: middle;
    padding: 1rem 0.75rem;
    border-bottom: 1px solid #f0f0f0;
    font-size: 0.875rem;
  }

  .table-responsive {
    overflow-x: visible;
  }

  .agent-id {
    max-width: 150px;
    display: inline-block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .table-row-hover {
    transition: all 0.2s ease-in-out;
    cursor: pointer;
  }

  .table-row-hover:hover {
    background-color: #f8f9fa;
    transform: translateX(2px);
    box-shadow: inset 3px 0 0 #0d6efd;
  }

  .cursor-pointer {
    cursor: pointer;
  }

  .font-monospace {
    font-family: var(--font-mono), monospace;
    font-size: 0.875rem;
  }

  .badge {
    font-weight: 500;
    padding: 0.35rem 0.65rem;
    font-size: 0.75rem;
    white-space: nowrap;
  }

  .badge.rounded-pill {
    padding: 0.35rem 0.85rem;
  }

  /* Responsive */
  @media (min-width: 576px) {
    .stats-grid {
      grid-template-columns: repeat(2, 1fr);
    }
  }

  @media (min-width: 768px) {
    .stats-grid {
      grid-template-columns: repeat(4, 1fr);
      gap: 1rem;
    }

    .stat-card {
      padding: 1.5rem;
    }

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
      padding-top: 0;
      border-top: none;
      margin-left: 1rem;
    }
  }

  @media (max-width: 576px) {
    .container-fluid {
      padding-left: 0.75rem;
      padding-right: 0.75rem;
    }

    .stat-number {
      font-size: 1.5rem;
    }

    .table-responsive {
      overflow-x: auto;
    }
  }
</style>
