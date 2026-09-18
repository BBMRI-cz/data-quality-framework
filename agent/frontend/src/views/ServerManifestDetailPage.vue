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

        <!-- Empty versions state -->
        <div v-if="versions.length === 0" class="alert alert-info mb-4">
          <i class="bi bi-clock-history me-2"></i>
          No versions published yet.
        </div>

        <template v-else>
          <!-- Version picker and info -->
          <div class="version-panel mb-4">
            <FormSelect
              v-model="selectedVersionId"
              label="Version"
              icon="bi bi-clock-history"
              :options="versionOptions"
              class="version-picker"
            />

            <div v-if="selectedVersion" class="version-meta">
              <div class="meta-item">
                <span class="meta-label">Generated At</span>
                <span>
                  {{ formatDateShort(selectedVersion.generatedAt) }}
                  {{ formatTime(selectedVersion.generatedAt) }}
                </span>
              </div>
              <div class="meta-item">
                <span class="meta-label">Key ID</span>
                <span>{{ selectedVersion.keyId || 'N/A' }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">Signature</span>
                <span class="signature-preview">{{
                  truncateText(selectedVersion.signature, 48)
                }}</span>
              </div>
              <div class="meta-item meta-actions">
                <button
                  class="btn btn-sm btn-outline-primary"
                  title="View signed body"
                  @click="showBodyModal = true"
                >
                  <i class="bi bi-braces"></i>
                  View Signed Body
                </button>
                <ActionButton
                  icon="bi bi-download"
                  :text="`Download v${selectedVersion.version}`"
                  :loading="downloading"
                  @click="downloadSelectedVersion"
                />
              </div>
            </div>
          </div>

          <!-- Quality checks of the selected version -->
          <ManifestQualityChecksTable
            :checks="qualityChecks"
            :loading="checksLoading"
            :error="checksError"
          />
        </template>
      </template>
    </div>

    <!-- Signed Body Modal -->
    <BaseModal
      :show="showBodyModal"
      :title="selectedVersion ? `Version v${selectedVersion.version}` : 'Manifest Version'"
      size="lg"
      :show-footer="false"
      @close="showBodyModal = false"
    >
      <template v-if="selectedVersion">
        <div class="version-meta mb-3">
          <div class="meta-item">
            <span class="meta-label">Signed At</span>
            <span>
              {{ formatDateShort(selectedVersion.generatedAt) }}
              {{ formatTime(selectedVersion.generatedAt) }}
            </span>
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
  import { ref, computed, watch, onMounted } from 'vue';
  import { useRoute } from 'vue-router';
  import PageHeader from '@/components/PageHeader.vue';
  import ActionButton from '@/components/ActionButton.vue';
  import StatCard from '@/components/StatCard.vue';
  import BaseModal from '@/components/BaseModal.vue';
  import FormSelect from '@/components/forms/FormSelect.vue';
  import ManifestQualityChecksTable from '@/components/manifests/ManifestQualityChecksTable.vue';
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
  const selectedVersionId = ref(null);
  const qualityChecks = ref([]);
  const checksLoading = ref(false);
  const checksError = ref(null);
  const downloading = ref(false);

  // Guards against out-of-order responses when the user switches versions quickly
  let checksRequestSeq = 0;

  const versions = computed(() => manifest.value?.versions || []);

  const sortedVersions = computed(() =>
    [...versions.value].sort((a, b) => new Date(b.generatedAt) - new Date(a.generatedAt))
  );

  const latestGeneratedAt = computed(() => {
    const latest = sortedVersions.value[0];
    return latest ? formatDateShort(latest.generatedAt) : '—';
  });

  const versionOptions = computed(() =>
    sortedVersions.value.map((version, index) => ({
      value: version.remoteId,
      label: `v${version.version} · ${formatDateShort(version.generatedAt)}${index === 0 ? ' (latest)' : ''}`,
    }))
  );

  const selectedVersion = computed(
    () =>
      sortedVersions.value.find((version) => version.remoteId === selectedVersionId.value) || null
  );

  const formattedBody = computed(() =>
    selectedVersion.value?.body ? JSON.stringify(selectedVersion.value.body, null, 2) : ''
  );

  watch(
    sortedVersions,
    (availableVersions) => {
      if (availableVersions.length > 0 && selectedVersionId.value == null) {
        selectedVersionId.value = availableVersions[0].remoteId;
      }
    },
    { immediate: true }
  );

  watch(selectedVersionId, (versionId) => {
    if (versionId != null) {
      loadQualityChecks(versionId);
    }
  });

  async function loadQualityChecks(versionId) {
    const requestSeq = ++checksRequestSeq;
    checksLoading.value = true;
    checksError.value = null;
    try {
      const checks = await manifestService.getVersionQualityChecks(serverId, manifestId, versionId);
      if (requestSeq === checksRequestSeq) {
        qualityChecks.value = checks;
      }
    } catch (err) {
      if (requestSeq === checksRequestSeq) {
        checksError.value =
          err.response?.data?.detail ||
          err.response?.data?.message ||
          'Unable to load the quality checks of this version. Please try again.';
        qualityChecks.value = [];
        notificationService.error('Load Failed', checksError.value);
      }
    } finally {
      if (requestSeq === checksRequestSeq) {
        checksLoading.value = false;
      }
    }
  }

  async function downloadSelectedVersion() {
    if (!selectedVersion.value) {
      return;
    }
    downloading.value = true;
    try {
      const result = await manifestService.downloadManifestVersion(
        serverId,
        manifestId,
        selectedVersion.value.version
      );
      notificationService.success(
        'Download Complete',
        `Installed v${result.installedVersion} of "${result.name}" with ${result.installedChecks} quality checks.`
      );
    } catch (err) {
      notificationService.error(
        'Download Failed',
        err.response?.data?.detail ||
          err.response?.data?.message ||
          'Unable to download this manifest version. Please try again.'
      );
    } finally {
      downloading.value = false;
    }
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

  .version-panel {
    display: grid;
    grid-template-columns: minmax(220px, 320px) 1fr;
    gap: var(--spacing-xl);
    align-items: start;
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
    padding: var(--spacing-lg);
  }

  .version-picker :deep(.form-field) {
    margin-bottom: 0;
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

  .meta-actions {
    margin-top: var(--spacing-xs);
    flex-direction: row;
    align-items: center;
    gap: var(--spacing-sm);
  }

  .meta-label {
    font-size: 0.75rem;
    font-weight: 600;
    color: var(--color-gray-500);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .signature-preview {
    font-family: var(--font-mono), monospace;
    font-size: 0.8rem;
    color: var(--color-gray-500);
    word-break: break-all;
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

    .version-panel {
      grid-template-columns: 1fr;
      gap: var(--spacing-md);
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
