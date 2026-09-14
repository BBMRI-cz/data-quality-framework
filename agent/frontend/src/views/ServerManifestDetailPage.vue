<template>
  <div class="server-manifest-detail-page">
    <PageHeader
      :title="manifest?.name || 'Manifest Details'"
      mobile-title="Manifest"
      :subtitle="server?.name ? `Signed versions published by ${server.name}` : 'Signed versions'"
      icon="bi bi-file-earmark-lock"
    />

    <div class="page-content">
      <!-- Back Button -->
      <div class="page-actions">
        <ActionButton
          :to="`/servers/${serverId}/manifests`"
          icon="bi bi-arrow-left"
          text="Back to Manifests"
          variant="secondary"
        />
      </div>

      <!-- Loading state -->
      <div v-if="loading" class="alert alert-info mb-4">
        <i class="bi bi-arrow-clockwise spinning me-2"></i>
        Loading manifest...
      </div>

      <!-- Error state -->
      <div v-else-if="error" class="alert alert-danger mb-4">
        <i class="bi bi-exclamation-triangle me-2"></i>
        {{ error }}
      </div>

      <template v-else-if="manifest">
        <!-- Stats Cards -->
        <div class="stats-grid mb-4">
          <StatCard
            :number="versions.length"
            label="Published Versions"
            number-class="text-primary"
            help-text="Signed versions of this manifest"
          />
          <StatCard
            :number="latestGeneratedAt"
            label="Latest Published"
            number-class="text-dark"
            help-text="Generation date of the latest version"
          />
        </div>

        <!-- Versions Table -->
        <BaseTable
          title="Versions"
          :columns="columns"
          :items="sortedVersions"
          :loading="false"
          item-key="version"
          item-label="versions"
          empty-text="No versions published yet"
          empty-icon="bi bi-clock-history"
        >
          <template #version="{ item }">
            <span class="badge bg-primary">v{{ item.version }}</span>
          </template>
          <template #generatedAt="{ item }">
            <div class="d-flex flex-column gap-1">
              <span class="fw-medium">{{ formatDateShort(item.generatedAt) }}</span>
              <span class="text-muted small">{{ formatTime(item.generatedAt) }}</span>
            </div>
          </template>
          <template #keyId="{ item }">
            <span class="text-muted">{{ item.keyId || 'N/A' }}</span>
          </template>
          <template #signature="{ item }">
            <span class="signature-preview">{{ truncateText(item.signature, 24) }}</span>
          </template>
          <template #actions="{ item }">
            <button
              class="btn btn-sm btn-outline-primary"
              title="View signed body"
              @click.stop="openBodyModal(item)"
            >
              <i class="bi bi-braces"></i>
              Body
            </button>
          </template>
        </BaseTable>
      </template>
    </div>

    <!-- Signed Body Modal -->
    <BaseModal
      :show="showBodyModal"
      :title="selectedVersion ? `Version v${selectedVersion.version}` : 'Manifest Version'"
      size="lg"
      :show-footer="false"
      @close="closeBodyModal"
    >
      <template v-if="selectedVersion">
        <div class="version-meta mb-3">
          <div class="meta-item">
            <span class="meta-label">Signed At</span>
            <span>{{ formatDateShort(selectedVersion.generatedAt) }} {{ formatTime(selectedVersion.generatedAt) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Key ID</span>
            <span>{{ selectedVersion.keyId || 'N/A' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Signature</span>
            <span class="signature-full">{{ selectedVersion.signature }}</span>
          </div>
        </div>
        <span class="meta-label">Signed Body</span>
        <pre class="json-viewer">{{ formattedBody }}</pre>
      </template>
    </BaseModal>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';
  import { useRoute } from 'vue-router';
  import PageHeader from '@/components/PageHeader.vue';
  import ActionButton from '@/components/ActionButton.vue';
  import StatCard from '@/components/StatCard.vue';
  import BaseTable from '@/components/BaseTable.vue';
  import BaseModal from '@/components/BaseModal.vue';
  import { serverService } from '@/services/serverService.js';
  import { manifestService } from '@/services/manifestService.js';
  import { notificationService } from '@/services/notificationService.js';
  import { truncateText, formatDateShort, formatTime } from '@/utils/stringUtils.js';

  const route = useRoute();

  const serverId = route.params.id;
  const manifestId = route.params.manifestId;
  const server = ref(null);
  const manifest = ref(null);
  const loading = ref(true);
  const error = ref(null);

  const showBodyModal = ref(false);
  const selectedVersion = ref(null);

  const columns = [
    { key: 'version', label: 'Version', headerClass: 'center', cellClass: 'center' },
    { key: 'generatedAt', label: 'Generated At' },
    { key: 'keyId', label: 'Key ID' },
    { key: 'signature', label: 'Signature' },
    { key: 'actions', label: '', headerClass: 'center', cellClass: 'center' },
  ];

  const versions = computed(() => manifest.value?.versions || []);

  const sortedVersions = computed(() =>
    [...versions.value].sort((a, b) => new Date(b.generatedAt) - new Date(a.generatedAt))
  );

  const latestGeneratedAt = computed(() => {
    const latest = sortedVersions.value[0];
    return latest ? formatDateShort(latest.generatedAt) : '—';
  });

  const formattedBody = computed(() =>
    selectedVersion.value?.body ? JSON.stringify(selectedVersion.value.body, null, 2) : ''
  );

  function openBodyModal(version) {
    selectedVersion.value = version;
    showBodyModal.value = true;
  }

  function closeBodyModal() {
    showBodyModal.value = false;
    selectedVersion.value = null;
  }

  async function loadManifest() {
    loading.value = true;
    error.value = null;
    try {
      manifest.value = await manifestService.getManifest(serverId, manifestId);
    } catch (err) {
      error.value =
        err.response?.data?.detail ||
        err.response?.data?.message ||
        'Unable to load the manifest from this server. Please try again.';
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
    loadManifest();
  });
</script>

<style scoped>
  .server-manifest-detail-page {
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
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: var(--spacing-md);
  }

  .badge {
    font-size: 0.75rem;
    font-weight: 600;
    padding: 0.35rem 0.65rem;
  }

  .signature-preview {
    font-family: var(--font-mono), monospace;
    font-size: 0.8rem;
    color: var(--color-gray-500);
  }

  .version-meta {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-sm);
  }

  .meta-item {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .meta-label {
    font-size: 0.75rem;
    font-weight: 600;
    color: var(--color-gray-500);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .signature-full {
    font-family: var(--font-mono), monospace;
    font-size: 0.8rem;
    word-break: break-all;
  }

  .json-viewer {
    background: var(--color-gray-50);
    border: 1px solid var(--color-gray-200);
    border-radius: var(--radius-md);
    padding: var(--spacing-md);
    overflow-x: auto;
    font-family: var(--font-mono), monospace;
    font-size: 0.875rem;
    line-height: 1.5;
    white-space: pre-wrap;
    word-wrap: break-word;
    margin: var(--spacing-xs) 0 0;
  }

  .alert {
    border: none;
    border-radius: var(--radius-md);
  }

  .alert-info {
    background-color: rgba(59, 130, 246, 0.1);
    color: #1e40af;
  }

  .alert-danger {
    background-color: rgba(239, 68, 68, 0.1);
    color: #991b1b;
  }

  .spinning {
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }

  @media (max-width: 768px) {
    .server-manifest-detail-page {
      padding: var(--spacing-md);
    }
  }

  @media (max-width: 576px) {
    .server-manifest-detail-page {
      padding: var(--spacing-sm);
    }

    .stats-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
