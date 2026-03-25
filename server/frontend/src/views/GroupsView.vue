<template>
  <div class="container-fluid py-3 py-md-4">
    <PageHeader
      title="Groups"
      mobile-title="Groups"
      subtitle="Manage agent groups"
      icon="bi bi-collection"
    >
      <template #actions>
        <button
          class="btn btn-outline-primary btn-sm"
          :disabled="loading"
          @click="refreshGroups"
        >
          <i class="bi bi-arrow-clockwise"></i>
          <span class="d-none d-md-inline ms-1">Refresh</span>
        </button>
        <button class="btn btn-primary btn-sm ms-2" :disabled="loading" @click="createGroup">
          <i class="bi bi-plus-lg"></i>
          <span class="d-none d-md-inline ms-1">New Group</span>
        </button>
      </template>
    </PageHeader>

    <div class="row g-3 mb-3 mb-md-4">
      <div class="col-12 col-sm-6 col-lg-4">
        <StatsCard
          label="Total Groups"
          :value="groups.length"
          icon="bi bi-collection"
        />
      </div>
    </div>

    <div class="mb-3 mb-md-4">
      <SearchBar v-model="searchQuery" placeholder="Search groups..." />
    </div>

    <PaginatedTable
      title="Group Definitions"
      :columns="tableColumns"
      :items="tableRows"
      :total-items="filteredGroups.length"
      :loading="loading"
      :error="error"
      :empty-title="emptyTitle"
      :empty-text="emptyText"
      item-key="id"
      item-label="groups"
      :paginate="false"
      @row-click="viewGroupDetail"
    >
      <template #header-meta>
        <Badge :text="`${filteredGroups.length} groups`" variant="secondary" size="small" />
      </template>

      <template #cell-name="{ value }">
        <span class="fw-medium">{{ value }}</span>
      </template>

      <template #cell-agentsCount="{ value }">
        <Badge :text="`${value} agents`" variant="primary" size="small" />
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
  import SearchBar from '@/components/ui/SearchBar.vue';
  import PaginatedTable from '@/components/ui/PaginatedTable.vue';
  import Badge from '@/components/ui/Badge.vue';

  const router = useRouter();
  const groups = ref([]);
  const loading = ref(false);
  const error = ref(null);
  const searchQuery = ref('');
  const tableColumns = [
    { key: 'name', label: 'Name' },
    { key: 'agentsCount', label: 'Agents' },
  ];

  const filteredGroups = computed(() => {
    if (!searchQuery.value) {
      return groups.value;
    }

    const query = searchQuery.value.toLowerCase();
    return groups.value.filter((group) => group.name?.toLowerCase().includes(query));
  });

  const tableRows = computed(() =>
    filteredGroups.value.map((group) => ({
      ...group,
      agentsCount: group.agentIds?.length || 0,
    }))
  );

  const emptyTitle = computed(() => 'No Groups Found');
  const emptyText = computed(() =>
    searchQuery.value ? 'Try adjusting your search criteria' : 'No groups are configured yet'
  );

  const loadGroups = async () => {
    loading.value = true;
    error.value = null;

    try {
      const data = await apiService.getGroups();
      // Handle HAL format response
      if (data._embedded && data._embedded.groups) {
        groups.value = data._embedded.groups;
      } else if (Array.isArray(data)) {
        groups.value = data;
      } else {
        groups.value = [];
      }
    } catch (err) {
      error.value = err.message || 'Failed to load groups';
      console.error('Error loading groups:', err);
    } finally {
      loading.value = false;
    }
  };

  const refreshGroups = () => {
    loadGroups();
  };

  const createGroup = () => {
    router.push('/groups/new');
  };

  const viewGroupDetail = (group) => {
    router.push(`/groups/${group.id}`);
  };

  onMounted(() => {
    loadGroups();
  });
</script>
