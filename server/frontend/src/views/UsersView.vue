<template>
  <div class="container-fluid py-3 py-md-4">
    <PageHeader
      title="Users"
      mobile-title="Users"
      subtitle="Manage system users"
      icon="bi bi-people"
    >
      <template #actions>
        <button
          class="btn btn-outline-primary btn-sm"
          :disabled="loading"
          @click="refreshUsers"
        >
          <i class="bi bi-arrow-clockwise"></i>
          <span class="d-none d-md-inline ms-1">Refresh</span>
        </button>
        <button
          class="btn btn-primary btn-sm ms-2"
          disabled
          title="User creation is currently disabled"
          @click="createUser"
        >
          <i class="bi bi-plus-lg"></i>
          <span class="d-none d-md-inline ms-1">New User</span>
        </button>
      </template>
    </PageHeader>

    <div class="row g-3 mb-3 mb-md-4">
      <div class="col-12 col-sm-6 col-lg-4">
        <StatsCard
          label="Total Users"
          :value="users.length"
          icon="bi bi-people"
        />
      </div>
    </div>

    <div class="mb-3 mb-md-4">
      <SearchBar v-model="searchQuery" placeholder="Search users..." />
    </div>

    <PaginatedTable
      title="System Users"
      :columns="tableColumns"
      :items="tableRows"
      :total-items="filteredUsers.length"
      :loading="loading"
      :error="error"
      :empty-title="emptyTitle"
      :empty-text="emptyText"
      item-key="id"
      item-label="users"
      :paginate="false"
      @row-click="viewUserDetail"
    >
      <template #header-meta>
        <Badge :text="`${filteredUsers.length} users`" variant="secondary" size="small" />
      </template>

      <template #cell-username="{ value }">
        <span class="fw-medium">{{ value }}</span>
      </template>

      <template #cell-subjectId="{ value }">
        <span v-if="value" class="text-muted">{{ value }}</span>
        <span v-else class="text-muted fst-italic">N/A</span>
      </template>

      <template #cell-roles="{ value }">
        <Badge v-for="role in value" :key="role" :text="role" variant="primary" size="small" />
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
  const users = ref([]);
  const loading = ref(false);
  const error = ref(null);
  const searchQuery = ref('');
  const tableColumns = [
    { key: 'username', label: 'Username' },
    { key: 'subjectId', label: 'Subject ID' },
    { key: 'roles', label: 'Roles' },
  ];

  const filteredUsers = computed(() => {
    if (!searchQuery.value) {
      return users.value;
    }

    const query = searchQuery.value.toLowerCase();
    return users.value.filter(
      (user) =>
        user.username?.toLowerCase().includes(query) || user.agentId?.toLowerCase().includes(query)
    );
  });

  const tableRows = computed(() =>
    filteredUsers.value.map((user) => ({
      ...user,
      roles: user.roles || [],
    }))
  );

  const emptyTitle = computed(() => 'No Users Found');
  const emptyText = computed(() =>
    searchQuery.value ? 'Try adjusting your search criteria' : 'No users are configured yet'
  );

  const loadUsers = async () => {
    loading.value = true;
    error.value = null;

    try {
      const data = await apiService.getUsers();
      // Handle HAL format response
      if (data._embedded && data._embedded.userDTOList) {
        users.value = data._embedded.userDTOList;
      } else if (Array.isArray(data)) {
        users.value = data;
      } else {
        users.value = [];
      }
    } catch (err) {
      error.value = err.message || 'Failed to load users';
      console.error('Error loading users:', err);
    } finally {
      loading.value = false;
    }
  };

  const refreshUsers = () => {
    loadUsers();
  };

  const createUser = () => {
    router.push('/users/new');
  };

  const viewUserDetail = (user) => {
    router.push(`/users/${user.id}`);
  };

  onMounted(() => {
    loadUsers();
  });
</script>
