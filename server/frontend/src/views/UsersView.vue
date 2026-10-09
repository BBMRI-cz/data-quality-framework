<template>
  <div class="container-fluid py-3 py-md-4">
    <PageHeader
      title="Users"
      mobile-title="Users"
      subtitle="Manage system users"
      icon="bi bi-people"
    >
      <template #actions>
        <button class="btn btn-outline-primary btn-sm" :disabled="loading" @click="refreshUsers">
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
          :value="totalUsers"
          icon="bi bi-people"
          color="var(--color-primary)"
        />
      </div>
    </div>

    <div class="mb-3 mb-md-4">
      <SearchBar v-model="searchQuery" placeholder="Search by username or subject ID..." />
    </div>

    <div class="mb-3 mb-md-4">
      <LabeledValuesFilter v-model="selectedRole" label="Roles:" :categories="roleOptions" />
    </div>

    <PaginatedTable
      title="System Users"
      :columns="tableColumns"
      :items="tableRows"
      :page="currentPage"
      :page-size="pageSize"
      :total-items="totalUsers"
      :loading="loading"
      :error="error"
      :empty-title="emptyTitle"
      :empty-text="emptyText"
      item-key="id"
      item-label="users"
      @row-click="viewUserDetail"
      @page-change="onPageChange"
    >
      <template #header-meta>
        <Badge :text="`${totalUsers} users`" variant="secondary" size="small" />
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
  import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { apiService } from '@/services/apiService.js';
  import PageHeader from '@/components/ui/PageHeader.vue';
  import StatsCard from '@/components/ui/StatsCard.vue';
  import SearchBar from '@/components/ui/SearchBar.vue';
  import LabeledValuesFilter from '@/components/ui/LabeledValuesFilter.vue';
  import PaginatedTable from '@/components/ui/PaginatedTable.vue';
  import Badge from '@/components/ui/Badge.vue';

  const SEARCH_DEBOUNCE_MS = 300;
  const DEFAULT_ROLE = 'HUMAN_USER';
  const roleOptions = ['HUMAN_USER', 'ADMIN'];

  const router = useRouter();
  const users = ref([]);
  const totalUsers = ref(0);
  const currentPage = ref(0);
  const pageSize = ref(10);
  const searchQuery = ref('');
  const selectedRole = ref(DEFAULT_ROLE);
  const loading = ref(false);
  const error = ref(null);
  let searchDebounceTimer = null;

  const tableColumns = [
    { key: 'username', label: 'Username' },
    { key: 'subjectId', label: 'Subject ID' },
    { key: 'roles', label: 'Roles' },
  ];

  const tableRows = computed(() =>
    users.value.map((user) => ({
      ...user,
      roles: user.roles || [],
    }))
  );

  const emptyTitle = computed(() => 'No Users Found');
  const emptyText = computed(() =>
    searchQuery.value ? 'Try adjusting your search criteria' : 'No users match the selected filters'
  );

  const buildQueryParams = () => {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      sort: 'username',
      order: 'ASC',
    };
    const query = searchQuery.value.trim();
    if (query) {
      params.search = query;
    }
    if (selectedRole.value) {
      params.roles = selectedRole.value;
    }
    return params;
  };

  const loadUsers = async () => {
    loading.value = true;
    error.value = null;

    try {
      const data = await apiService.getUsers(buildQueryParams());
      // Handle HAL format response
      users.value = data?._embedded?.userDTOList || (Array.isArray(data) ? data : []);
      totalUsers.value = data?.page?.totalElements ?? users.value.length;
    } catch (err) {
      error.value = err.message || 'Failed to load users';
      console.error('Error loading users:', err);
    } finally {
      loading.value = false;
    }
  };

  const reloadFromFirstPage = () => {
    currentPage.value = 0;
    loadUsers();
  };

  const refreshUsers = () => {
    reloadFromFirstPage();
  };

  const onPageChange = (page) => {
    currentPage.value = page;
    loadUsers();
  };

  const createUser = () => {
    router.push('/users/new');
  };

  const viewUserDetail = (user) => {
    router.push(`/users/${user.id}`);
  };

  watch(searchQuery, () => {
    clearTimeout(searchDebounceTimer);
    searchDebounceTimer = setTimeout(reloadFromFirstPage, SEARCH_DEBOUNCE_MS);
  });

  watch(selectedRole, reloadFromFirstPage);

  onMounted(loadUsers);

  onUnmounted(() => clearTimeout(searchDebounceTimer));
</script>
