<template>
  <div class="server-manifests-page">
    <PageHeader
      :title="server?.name ? `Manifests · ${server.name}` : 'Server Manifests'"
      mobile-title="Manifests"
      subtitle="Browse quality check manifests published by this central server"
      icon="bi bi-file-earmark-lock"
    >
      <template #actions>
        <ActionButton
          icon="bi bi-arrow-clockwise"
          text="Refresh"
          variant="secondary"
          :loading="loading"
          @click="loadManifests"
        />
      </template>
    </PageHeader>

    <div class="page-content">
      <div class="page-actions">
        <ActionButton
          :to="`/servers/${serverId}`"
          icon="bi bi-arrow-left"
          text="Back to Server"
          variant="secondary"
        />
      </div>

      <!-- Error state -->
      <div v-if="error" class="alert alert-danger mb-4">
        <i class="bi bi-exclamation-triangle me-2"></i>
        {{ error }}
      </div>

      <div class="stats-grid">
        <StatCard
          :number="manifests.length"
          label="Total Manifests"
          number-class="text-primary"
          help-text="Manifests published by this server"
        />
        <StatCard
          :number="totalVersions"
          label="Published Versions"
          number-class="text-success"
          help-text="Signed versions across all manifests"
        />
      </div>

      <div class="search-bar">
        <i class="bi bi-search search-icon"></i>
        <input
          v-model="searchQuery"
          type="text"
          class="form-control"
          placeholder="Search manifests..."
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
        title="Manifests"
        :columns="columns"
        :items="filteredManifests"
        :loading="loading"
        item-key="id"
        item-label="manifests"
        :empty-text="
          searchQuery ? 'No manifests match your search' : 'No manifests published by this server'
        "
        empty-icon="bi bi-file-earmark-lock"
        @row-click="navigateToDetails"
      >
        <template #name="{ item }">
          <span class="fw-medium">{{ item.name }}</span>
        </template>
        <template #versionCount="{ item }">
          <span class="badge bg-secondary">{{ item.versionCount }}</span>
        </template>
        <template #latestVersion="{ item }">
          <span v-if="item.latestVersion != null" class="badge bg-primary"
            >v{{ item.latestVersion }}</span
          >
          <span v-else class="text-muted">—</span>
        </template>
        <template #latestPublished="{ item }">
          <div v-if="item.latestPublished" class="d-flex flex-column gap-1">
            <span class="fw-medium">{{ formatDateShort(item.latestPublished) }}</span>
            <span class="text-muted small">{{ formatTime(item.latestPublished) }}</span>
          </div>
          <span v-else class="text-muted">—</span>
        </template>
      </BaseTable>
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import PageHeader from '@/components/PageHeader.vue';
  import ActionButton from '@/components/ActionButton.vue';
  import StatCard from '@/components/StatCard.vue';
  import BaseTable from '@/components/BaseTable.vue';
  import { serverService } from '@/services/serverService.js';
  import { manifestService } from '@/services/manifestService.js';
  import { notificationService } from '@/services/notificationService.js';
  import { formatDateShort, formatTime } from '@/utils/stringUtils.js';

  const route = useRoute();
  const router = useRouter();

  const serverId = route.params.id;
  const server = ref(null);
  const manifests = ref([]);
  const loading = ref(true);
  const error = ref(null);
  const searchQuery = ref('');

  const columns = [
    { key: 'id', label: 'ID' },
    { key: 'name', label: 'Name' },
    { key: 'versionCount', label: 'Versions', headerClass: 'center', cellClass: 'center' },
    { key: 'latestVersion', label: 'Latest', headerClass: 'center', cellClass: 'center' },
    { key: 'latestPublished', label: 'Latest Published' },
  ];

  function latestVersionOf(versions) {
    if (!versions || versions.length === 0) return null;
    return versions.reduce((latest, version) =>
      new Date(version.generatedAt) > new Date(latest.generatedAt) ? version : latest
    );
  }

  const mappedManifests = computed(() =>
    manifests.value.map((manifest) => {
      const versions = manifest.versions || [];
      const latest = latestVersionOf(versions);
      return {
        id: manifest.remoteId,
        name: manifest.name,
        versionCount: versions.length,
        latestVersion: latest ? latest.version : null,
        latestPublished: latest ? latest.generatedAt : null,
      };
    })
  );

  const totalVersions = computed(() =>
    mappedManifests.value.reduce((sum, manifest) => sum + manifest.versionCount, 0)
  );

  const filteredManifests = computed(() => {
    if (!searchQuery.value) {
      return mappedManifests.value;
    }
    const query = searchQuery.value.toLowerCase();
    return mappedManifests.value.filter((manifest) => manifest.name?.toLowerCase().includes(query));
  });

  function navigateToDetails(manifest) {
    router.push(`/servers/${serverId}/manifests/${manifest.id}`);
  }

  async function loadManifests() {
    loading.value = true;
    error.value = null;
    try {
      manifests.value = await manifestService.getManifests(serverId);
    } catch (err) {
      error.value =
        err.response?.data?.detail ||
        err.response?.data?.message ||
        'Unable to load manifests from this server. Please try again.';
      notificationService.error('Load Failed', error.value);
    } finally {
      loading.value = false;
    }
  }

  async function loadServer() {
    try {
      server.value = await serverService.get(serverId);
    } catch (err) {
      console.error('Error loading server details:', err);
    }
  }

  onMounted(() => {
    loadServer();
    loadManifests();
  });
</script>

<style scoped>
  .server-manifests-page {
    min-height: 100%;
    padding: var(--spacing-xl);
  }

  .page-content {
    width: 100%;
  }

  .page-actions {
    display: flex;
    justify-content: flex-start;
    margin-bottom: var(--spacing-md);
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
    padding: 0.35rem 0.65rem;
  }

  .alert {
    border: none;
    border-radius: var(--radius-md);
  }

  .alert-danger {
    background-color: rgba(239, 68, 68, 0.1);
    color: #991b1b;
  }

  @media (max-width: 768px) {
    .server-manifests-page {
      padding: var(--spacing-md);
    }

    .search-bar {
      max-width: 100%;
    }
  }

  @media (max-width: 576px) {
    .server-manifests-page {
      padding: var(--spacing-sm);
    }

    .stats-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
