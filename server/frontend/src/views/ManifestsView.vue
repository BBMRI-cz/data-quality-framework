<template>
  <div class="container-fluid py-3 py-md-4">
    <PageHeader
      title="Manifests"
      mobile-title="Manifests"
      subtitle="View and manage quality check manifests"
      icon="bi bi-file-earmark-lock"
    >
      <template #actions>
        <button class="btn btn-outline-primary btn-sm" :disabled="loading" @click="loadManifests">
          <i class="bi bi-arrow-clockwise"></i>
          <span class="d-none d-md-inline ms-1">Refresh</span>
        </button>
        <button class="btn btn-primary btn-sm ms-2" :disabled="loading" @click="showCreate = true">
          <i class="bi bi-plus-lg"></i>
          <span class="d-none d-md-inline ms-1">New Manifest</span>
        </button>
      </template>
    </PageHeader>

    <div class="row g-3 mb-3 mb-md-4">
      <div class="col-12 col-sm-6 col-lg-4">
        <StatsCard
          label="Total Manifests"
          :value="manifests.length"
          icon="bi bi-file-earmark-lock"
          color="var(--color-primary)"
        />
      </div>
    </div>

    <div class="mb-3 mb-md-4">
      <SearchBar v-model="searchQuery" placeholder="Search manifests..." />
    </div>

    <PaginatedTable
      title="Manifests"
      :columns="tableColumns"
      :items="filteredManifests"
      :total-items="filteredManifests.length"
      :loading="loading"
      :error="error"
      :empty-title="emptyTitle"
      :empty-text="emptyText"
      item-key="id"
      item-label="manifests"
      :paginate="false"
      @row-click="viewManifestDetail"
    >
      <template #header-meta>
        <Badge :text="`${filteredManifests.length} manifests`" variant="secondary" size="small" />
      </template>

      <template #cell-name="{ value }">
        <span class="fw-medium">{{ value }}</span>
      </template>

      <template #cell-versionCount="{ item }">
        <Badge :text="`${item.versionCount}`" variant="primary" size="small" />
      </template>

      <template #cell-latestVersion="{ item }">
        <span v-if="item.latestVersion" class="badge bg-primary">v{{ item.latestVersion }}</span>
        <span v-else class="text-muted">—</span>
      </template>
    </PaginatedTable>

    <BaseModal
      :show="showCreate"
      title="New Manifest"
      subtitle="Create a new quality check manifest"
      icon="bi bi-file-earmark-lock"
      :loading="creating"
      :save-disabled="!newName.trim()"
      :save-button-props="{ text: 'Create' }"
      @close="closeCreateModal"
      @save="createManifest"
    >
      <div class="py-1">
        <label class="form-label fw-semibold">Name</label>
        <input
          v-model="newName"
          type="text"
          class="form-control"
          :class="{ 'is-invalid': nameError }"
          placeholder="e.g. Quality Checks 2026-09"
          :disabled="creating"
          @keyup.enter="createManifest"
        />
        <div v-if="nameError" class="invalid-feedback">{{ nameError }}</div>
        <small class="form-text text-muted mt-2">
          <i class="bi bi-info-circle me-1"></i>
          You can publish signed versions of the manifest from its detail page.
        </small>
      </div>
    </BaseModal>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { apiService } from '@/services/apiService.js';
  import { notificationService } from '@/services/notificationService.js';
  import PageHeader from '@/components/ui/PageHeader.vue';
  import StatsCard from '@/components/ui/StatsCard.vue';
  import SearchBar from '@/components/ui/SearchBar.vue';
  import PaginatedTable from '@/components/ui/PaginatedTable.vue';
  import Badge from '@/components/ui/Badge.vue';
  import BaseModal from '@/components/BaseModal.vue';

  const router = useRouter();
  const manifests = ref([]);
  const loading = ref(false);
  const error = ref(null);
  const searchQuery = ref('');

  const showCreate = ref(false);
  const newName = ref('');
  const creating = ref(false);
  const nameError = ref('');

  const tableColumns = [
    { key: 'id', label: 'ID' },
    { key: 'name', label: 'Name' },
    { key: 'versionCount', label: 'Versions' },
    { key: 'latestVersion', label: 'Latest' },
  ];

  const mappedManifests = computed(() =>
    manifests.value.map((manifest) => {
      const versions = manifest.versions || [];
      const latest = versions[versions.length - 1] || null;
      return {
        id: manifest.id,
        name: manifest.name,
        versionCount: versions.length,
        latestVersion: latest ? latest.version : null,
      };
    })
  );

  const filteredManifests = computed(() => {
    if (!searchQuery.value) {
      return mappedManifests.value;
    }
    const query = searchQuery.value.toLowerCase();
    return mappedManifests.value.filter((manifest) => manifest.name?.toLowerCase().includes(query));
  });

  const emptyTitle = computed(() => 'No Manifests Found');
  const emptyText = computed(() =>
    searchQuery.value ? 'Try adjusting your search criteria' : 'No manifests are configured yet'
  );

  const loadManifests = async () => {
    loading.value = true;
    error.value = null;
    try {
      const data = await apiService.getManifests();
      manifests.value = data._embedded?.manifests || (Array.isArray(data) ? data : []);
    } catch (err) {
      error.value = err.message || 'Failed to load manifests';
      console.error('Error loading manifests:', err);
    } finally {
      loading.value = false;
    }
  };

  const resetCreateModal = () => {
    showCreate.value = false;
    newName.value = '';
    nameError.value = '';
  };

  const closeCreateModal = () => {
    if (creating.value) return;
    resetCreateModal();
  };

  const createManifest = async () => {
    const name = newName.value.trim();
    nameError.value = name ? '' : 'Name is required';
    if (!name || creating.value) return;

    creating.value = true;
    try {
      await apiService.createManifest({ name });
      resetCreateModal();
      notificationService.success('Manifest Created', `Manifest "${name}" was created`);
      await loadManifests();
    } catch (err) {
      console.error('Error creating manifest:', err);
      notificationService.error('Create Failed', err.message || 'Failed to create manifest');
    } finally {
      creating.value = false;
    }
  };

  const viewManifestDetail = (manifest) => {
    router.push(`/manifests/${manifest.id}`);
  };

  onMounted(loadManifests);
</script>
