<template>
  <div class="servers-page">
    <PageHeader
      title="Central Servers"
      mobile-title="Servers"
      subtitle="Manage central servers that receive quality reports and publish quality checks"
      icon="bi bi-hdd-network"
    >
      <template #actions>
        <ActionButton to="/servers/new" icon="bi bi-plus" text="Add Central Server" />
      </template>
    </PageHeader>

    <div class="page-content">
      <div class="stats-grid">
        <StatCard
          :number="servers.length"
          label="Total Servers"
          number-class="text-primary"
          help-text="Number of registered central servers"
        />
        <StatCard
          :number="activeServers"
          label="Active Servers"
          number-class="text-success"
          help-text="Servers that currently receive reports and publish quality checks"
        />
      </div>

      <div class="search-bar">
        <i class="bi bi-search search-icon"></i>
        <input
          v-model="searchQuery"
          type="text"
          class="form-control"
          placeholder="Search servers..."
        />
        <button
          v-if="searchQuery"
          class="btn btn-link clear-btn"
          type="button"
          @click="searchQuery = ''"
        >
          <i class="bi bi-x-circle"></i>
        </button>
      </div>

      <BaseTable
        title="Central Servers"
        :columns="columns"
        :items="filteredServers"
        :loading="serverStore.loading"
        item-key="id"
        item-label="servers"
        empty-text="No central servers registered yet"
        empty-icon="bi bi-hdd-network"
        @row-click="navigateToDetails"
      >
        <template #name="{ item }">
          <span class="fw-medium">{{ item.name }}</span>
        </template>
        <template #url="{ item }">
          <span class="text-muted">{{ item.url }}</span>
        </template>
        <template #status="{ item }">
          <span
            :class="['badge', getStatusBadgeClass(item.status)]"
            :title="getStatusTooltip(item.status)"
          >
            <i :class="getStatusIcon(item.status)"></i>
            {{ formatStatus(item.status) }}
          </span>
        </template>
        <template #actions="{ item }">
          <button
            class="btn btn-sm btn-outline-danger"
            title="Remove server"
            @click.stop="openDeleteModal(item)"
          >
            <i class="bi bi-trash"></i>
          </button>
        </template>
      </BaseTable>
    </div>

    <DeleteConfirmModal
      v-if="showDeleteModal && deletingServer"
      :item-name="deletingServer.name"
      :loading="isDeleting"
      @close="closeDeleteModal"
      @confirm="deleteServer"
    />
  </div>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { useServerStore } from '@/stores/serverStore.js';
  import PageHeader from '@/components/PageHeader.vue';
  import ActionButton from '@/components/ActionButton.vue';
  import StatCard from '@/components/StatCard.vue';
  import BaseTable from '@/components/BaseTable.vue';
  import DeleteConfirmModal from '@/components/DeleteConfirmModal.vue';
  import { notificationService } from '@/services/notificationService.js';
  import {
    getStatusBadgeClass,
    getStatusIcon,
    formatStatus,
    getStatusTooltip,
  } from '@/utils/serverStatus.js';

  const router = useRouter();
  const serverStore = useServerStore();

  const searchQuery = ref('');
  const isDeleting = ref(false);
  const showDeleteModal = ref(false);
  const deletingServer = ref(null);

  const columns = [
    { key: 'name', label: 'Name' },
    { key: 'url', label: 'URL' },
    { key: 'status', label: 'Status', headerClass: 'center', cellClass: 'center' },
    { key: 'actions', label: '', headerClass: 'center', cellClass: 'center' },
  ];

  const servers = computed(() => serverStore.servers);
  const activeServers = computed(
    () => servers.value.filter((server) => server.status === 'ACTIVE').length
  );

  const filteredServers = computed(() => {
    if (!searchQuery.value) {
      return servers.value;
    }
    const query = searchQuery.value.toLowerCase();
    return servers.value.filter(
      (server) =>
        server.name?.toLowerCase().includes(query) || server.url?.toLowerCase().includes(query)
    );
  });

  function navigateToDetails(server) {
    router.push(`/servers/${server.id}`);
  }

  function openDeleteModal(server) {
    deletingServer.value = server;
    showDeleteModal.value = true;
  }

  function closeDeleteModal() {
    showDeleteModal.value = false;
    deletingServer.value = null;
  }

  async function deleteServer() {
    if (!deletingServer.value) return;

    isDeleting.value = true;
    try {
      await serverStore.deleteServer(deletingServer.value.id);
      notificationService.success(
        'Server Deleted',
        `${deletingServer.value.name} has been deleted successfully`
      );
      closeDeleteModal();
    } catch {
      notificationService.error(
        'Delete Failed',
        serverStore.error || 'Unable to delete server. Please try again.'
      );
    } finally {
      isDeleting.value = false;
    }
  }

  async function loadServers() {
    try {
      await serverStore.fetchServers();
    } catch {
      notificationService.error('Load Failed', 'Unable to load servers. Please try again.');
    }
  }

  onMounted(loadServers);
</script>

<style scoped>
  .servers-page {
    min-height: 100%;
    padding: var(--spacing-xl);
  }

  .page-content {
    width: 100%;
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: var(--spacing-md);
    margin-bottom: var(--spacing-lg);
  }

  .search-bar {
    position: relative;
    margin-bottom: var(--spacing-lg);
    max-width: 400px;
  }

  .search-bar .form-control {
    padding-left: 2.5rem;
    padding-right: 2.5rem;
    border: 2px solid var(--color-gray-200);
    border-radius: var(--radius-md);
    background: var(--bg-card);
  }

  .search-bar .form-control:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
  }

  .search-icon {
    position: absolute;
    left: 0.875rem;
    top: 50%;
    transform: translateY(-50%);
    color: var(--color-gray-400);
    font-size: 1rem;
    pointer-events: none;
  }

  .clear-btn {
    position: absolute;
    right: 0.25rem;
    top: 50%;
    transform: translateY(-50%);
    color: var(--color-gray-400);
    padding: 0.25rem 0.5rem;
    text-decoration: none;
  }

  .clear-btn:hover {
    color: var(--color-gray-600);
  }

  .badge {
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    padding: 0.35rem 0.65rem;
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
  }

  @media (max-width: 768px) {
    .servers-page {
      padding: var(--spacing-md);
    }

    .search-bar {
      max-width: 100%;
    }
  }

  @media (max-width: 576px) {
    .servers-page {
      padding: var(--spacing-sm);
    }

    .stats-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
