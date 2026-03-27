<template>
  <div class="container-fluid py-3 py-md-4">
    <PageHeader
      title="Agents Management"
      mobile-title="Agents"
      subtitle="Manage and monitor all connected agents in the network"
      icon="bi bi-database-fill-gear"
    >
      <template #actions>
        <button class="btn btn-outline-primary btn-sm" :disabled="loading" @click="refreshAgents">
          <i class="bi bi-arrow-clockwise"></i>
          <span class="d-none d-md-inline ms-1">Refresh</span>
        </button>
      </template>
    </PageHeader>

    <div class="row g-3 mb-3 mb-md-4">
      <div class="col-6 col-md-3">
        <StatsCard
          label="Total"
          :value="agentStats.total"
          icon="bi bi-database-fill-gear"
          color="var(--color-primary)"
        />
      </div>
      <div class="col-6 col-md-3">
        <StatsCard
          label="Active"
          :value="agentStats.active"
          icon="bi bi-check-circle-fill"
          color="var(--color-success)"
        />
      </div>
      <div class="col-6 col-md-3">
        <StatsCard
          label="Pending"
          :value="agentStats.pending"
          icon="bi bi-exclamation-circle-fill"
          color="var(--color-warning)"
        />
      </div>
      <div class="col-6 col-md-3">
        <StatsCard
          label="Inactive"
          :value="agentStats.inactive"
          icon="bi bi-dash-circle-fill"
          color="var(--color-gray-500)"
        />
      </div>
    </div>

    <PaginatedTable
      title="Agents"
      :columns="tableColumns"
      :items="tableRows"
      :total-items="filteredAgents.length"
      :loading="loading"
      :error="error"
      :empty-title="emptyTitle"
      :empty-text="emptyText"
      item-key="id"
      item-label="agents"
      :paginate="false"
      @row-click="navigateToAgentReports"
    >
      <template #header-meta>
        <Badge :text="`${filteredAgents.length} agents`" variant="secondary" size="small" />
      </template>

      <template #cell-groups="{ value }">
        <span v-if="!value.length" class="text-muted fst-italic">N/A</span>
        <Badge v-for="group in value" v-else :key="group" :text="group" size="small" />
      </template>

      <template #cell-status="{ item, value }">
        <div class="d-flex align-items-center gap-1">
          <Badge :text="value" :color="item.statusColor" size="small" />
        </div>
      </template>
    </PaginatedTable>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { apiService } from '@/services/apiService.js';
  import PageHeader from '@/components/ui/PageHeader.vue';
  import StatsCard from '@/components/ui/StatsCard.vue';
  import PaginatedTable from '@/components/ui/PaginatedTable.vue';
  import Badge from '@/components/ui/Badge.vue';

  const router = useRouter();
  const loading = ref(true);
  const error = ref(null);
  const agents = ref([]);

  const tableColumns = [
    { key: 'name', label: 'Name' },
    { key: 'id', label: 'ID' },
    { key: 'version', label: 'Version' },
    { key: 'groups', label: 'Groups' },
    { key: 'status', label: 'Status' },
  ];

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

  const filteredAgents = computed(() => agents.value);

  const getStatusMeta = (status) => {
    switch (status) {
      case 'ACTIVE':
        return {
          color: 'var(--color-success)',
          icon: 'bi bi-check-circle-fill',
          iconStyle: { color: 'var(--color-success)' },
        };
      case 'PENDING':
        return {
          color: 'var(--color-warning)',
          icon: 'bi bi-exclamation-circle-fill',
          iconStyle: { color: 'var(--color-warning)' },
        };
      case 'INACTIVE':
        return {
          color: 'var(--color-gray-500)',
          icon: 'bi bi-dash-circle-fill',
          iconStyle: { color: 'var(--color-gray-500)' },
        };
      case 'ERROR':
        return {
          color: 'var(--color-danger)',
          icon: 'bi bi-x-circle-fill',
          iconStyle: { color: 'var(--color-danger)' },
        };
      default:
        return {
          color: 'var(--color-gray-500)',
          icon: 'bi bi-question-circle-fill',
          iconStyle: { color: 'var(--color-gray-500)' },
        };
    }
  };

  const tableRows = computed(() => {
    return filteredAgents.value.map((agent) => {
      const statusMeta = getStatusMeta(agent.status);
      const groups = Array.isArray(agent.groups)
        ? agent.groups
            .map((group) => {
              if (typeof group === 'string') return group;
              return group?.name || group?.id || '';
            })
            .filter(Boolean)
        : Array.isArray(agent.groupIds)
          ? agent.groupIds
          : [];

      return {
        ...agent,
        name: agent.name || 'Unknown',
        version: agent.version || 'N/A',
        groups,
        statusColor: statusMeta.color,
        statusIcon: statusMeta.icon,
        statusIconStyle: statusMeta.iconStyle,
      };
    });
  });

  const emptyTitle = computed(() => 'No Agents Found');
  const emptyText = computed(() => 'No agents are currently registered in the system');

  const navigateToAgentReports = (agent) => {
    router.push(`/agents/${agent.id}/reports`);
  };

  onMounted(fetchAgents);
</script>
