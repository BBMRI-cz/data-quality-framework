<template>
  <div class="container-fluid px-2 px-md-3 py-3 py-md-4">
    <div class="row">
      <div class="col-12">
        <!-- Page Header -->
        <PageHeader
          title="Publish New Version"
          mobile-title="Publish"
          :subtitle="manifest?.name ? `Manifest: ${manifest.name}` : 'Manifest Versions'"
          icon="bi bi-file-earmark-lock"
        >
          <template #actions>
            <button
              class="btn btn-primary btn-sm"
              :disabled="selectedHashes.length === 0 || publishing || loadingChecks"
              @click="requestPublish"
            >
              <span
                v-if="publishing"
                class="spinner-border spinner-border-sm me-2"
                role="status"
              ></span>
              <i v-else class="bi bi-plus-lg"></i>
              <span class="d-none d-md-inline ms-1">Publish Version</span>
            </button>
          </template>
        </PageHeader>

        <!-- Back Button -->
        <div class="mb-3">
          <button class="btn btn-outline-secondary btn-sm" @click="goBack">
            <i class="bi bi-arrow-left me-2"></i>Back to Manifest
          </button>
        </div>

        <!-- Loading State -->
        <div v-if="loadingChecks" class="loading-state">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading quality checks...</span>
          </div>
        </div>

        <!-- Error State -->
        <div v-else-if="checkError" class="alert alert-danger" role="alert">
          <h6 class="alert-heading">Error Loading Quality Checks</h6>
          <p class="mb-0">{{ checkError }}</p>
        </div>

        <div v-else-if="availableChecks.length === 0" class="card border-0 shadow-sm">
          <div class="card-body p-4 text-muted">
            <i class="bi bi-inbox me-1"></i>No quality checks with published versions available
          </div>
        </div>

        <!-- Selection Card -->
        <div v-else class="card border-0 shadow-sm mb-4">
          <div class="card-header bg-white border-bottom py-3">
            <h5 class="mb-0 fw-semibold">
              <i class="bi bi-check-square text-primary me-2"></i>
              Select Quality Checks
              <span v-if="selectedHashes.length > 0" class="badge bg-primary ms-2 align-middle">
                {{ selectedHashes.length }}
              </span>
            </h5>
          </div>
          <div class="card-body p-4">
            <p class="text-muted small mb-3">
              <i class="bi bi-info-circle me-1"></i>
              Select the quality checks to include and expand a row to pick a specific version.
            </p>
            <div class="table-responsive">
              <table class="table table-hover table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th style="width: 2rem"></th>
                    <th style="width: 2rem"></th>
                    <th>Quality Check</th>
                    <th style="width: 6rem">Version</th>
                    <th>Query</th>
                    <th>Hash</th>
                  </tr>
                </thead>
                <tbody>
                  <template v-for="check in availableChecks" :key="check.id">
                    <tr :class="{ 'table-primary': isSelected(check) }">
                      <td>
                        <button
                          type="button"
                          class="btn btn-link btn-sm p-0 text-muted expand-btn"
                          :disabled="publishing"
                          @click="toggleExpand(check)"
                        >
                          <i
                            :class="
                              isExpanded(check) ? 'bi bi-chevron-down' : 'bi bi-chevron-right'
                            "
                          ></i>
                        </button>
                      </td>
                      <td>
                        <input
                          type="checkbox"
                          class="form-check-input"
                          :checked="isSelected(check)"
                          :disabled="publishing"
                          @change="toggleCheck(check)"
                        />
                      </td>
                      <td>
                        <button
                          type="button"
                          class="btn btn-link btn-sm p-0 text-decoration-none check-name"
                          @click="toggleExpand(check)"
                        >
                          <span class="fw-medium">{{ check.name }}</span>
                        </button>
                        <div v-if="check.category" class="text-muted small">
                          {{ check.category.name }}
                        </div>
                      </td>
                      <td>
                        <span class="badge bg-primary">v{{ versionFor(check) }}</span>
                      </td>
                      <td class="query-cell">
                        <span class="font-monospace small text-muted">{{
                          truncate(queryFor(check))
                        }}</span>
                        <button
                          v-if="queryFor(check)"
                          type="button"
                          class="btn btn-link btn-sm p-0 ms-2 text-muted"
                          title="View full query"
                          @click="openQueryModal(check, versionFor(check))"
                        >
                          <i class="bi bi-arrows-fullscreen"></i>
                        </button>
                      </td>
                      <td>
                        <code class="font-monospace small text-muted">{{
                          truncate(hashFor(check), 16)
                        }}</code>
                      </td>
                    </tr>
                    <tr v-if="isExpanded(check)" class="versions-row">
                      <td colspan="6" class="p-0">
                        <div class="versions-panel">
                          <div
                            v-for="v in check.versions"
                            :key="v.version"
                            class="version-item"
                            :class="{ active: versionFor(check) === v.version }"
                          >
                            <input
                              type="radio"
                              class="form-check-input me-2"
                              :name="'check-' + check.id"
                              :checked="versionFor(check) === v.version"
                              :disabled="publishing"
                              @change="onVersionChange(check, v.version)"
                            />
                            <span class="badge bg-primary">v{{ v.version }}</span>
                            <span class="query-preview font-monospace small text-muted">{{
                              truncate(v.query)
                            }}</span>
                            <button
                              v-if="v.query"
                              type="button"
                              class="btn btn-link btn-sm p-0 me-2 text-muted"
                              title="View full query"
                              @click="openQueryModal(check, v.version)"
                            >
                              <i class="bi bi-arrows-fullscreen"></i>
                            </button>
                            <code class="font-monospace small text-muted ms-auto">{{
                              truncate(v.hash, 16)
                            }}</code>
                          </div>
                        </div>
                      </td>
                    </tr>
                  </template>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>

    <ConfirmModal
      :show="showConfirm"
      title="Publish New Version"
      subtitle="Confirm version publication"
      message="You are about to publish a new signed version of this manifest with the following checks:"
      confirm-text="Yes, Publish"
      :loading="publishing"
      @close="cancelPublish"
      @confirm="confirmPublish"
    >
      <template #body>
        <div class="py-2">
          <div class="hashes-list text-start">
            <div v-for="item in pendingItems" :key="item.hash" class="hash-row">
              <div class="hash-row-info">
                <span class="fw-medium">{{ item.name }}</span>
                <span class="badge bg-primary ms-1">v{{ item.version }}</span>
              </div>
              <code class="hash-value font-monospace small">{{ item.hash }}</code>
            </div>
          </div>
          <p class="text-muted small mt-2 mb-0">
            <i class="bi bi-info-circle me-1"></i>
            The new version will be immutable once published.
          </p>
        </div>
      </template>
    </ConfirmModal>

    <BaseModal
      :show="showQueryModal"
      title="Quality Check Query"
      icon="bi bi-code-slash"
      size="lg"
      :show-footer="false"
      @close="showQueryModal = false"
    >
      <div v-if="queryModalCheck" class="mb-3">
        <h6 class="fw-semibold mb-1">{{ queryModalCheck.name }}</h6>
        <span class="badge bg-primary">v{{ queryModalVersion }}</span>
      </div>
      <pre class="query-full font-monospace bg-light rounded p-3 mb-0">{{ fullQuery }}</pre>
    </BaseModal>
  </div>
</template>

<script setup>
  import { ref, reactive, computed, onMounted } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { useHead } from '@unhead/vue';
  import { apiService } from '@/services/apiService.js';
  import { notificationService } from '@/services/notificationService.js';
  import PageHeader from '@/components/ui/PageHeader.vue';
  import ConfirmModal from '@/components/ConfirmModal.vue';
  import BaseModal from '@/components/BaseModal.vue';

  const QUERY_PREVIEW_LENGTH = 80;

  const route = useRoute();
  const router = useRouter();

  const manifestId = ref(route.params.id);
  const manifest = ref(null);

  const qualityChecks = ref([]);
  const loadingChecks = ref(false);
  const checkError = ref(null);
  const expanded = reactive({});
  const selected = reactive({});
  const chosenVersion = reactive({});

  const publishing = ref(false);
  const showConfirm = ref(false);
  const pendingItems = ref([]);

  const showQueryModal = ref(false);
  const queryModalCheck = ref(null);
  const queryModalVersion = ref(null);
  const fullQuery = ref('');

  useHead({ title: 'Publish Manifest Version' });

  const availableChecks = computed(() =>
    qualityChecks.value.filter((check) => check.versions && check.versions.length > 0)
  );

  const truncate = (text) => {
    if (!text) return '';
    return text.length > QUERY_PREVIEW_LENGTH
      ? text.slice(0, QUERY_PREVIEW_LENGTH).trimEnd() + '…'
      : text;
  };

  const isSelected = (check) => !!selected[check.id];

  const isExpanded = (check) => !!expanded[check.id];

  const latestVersion = (check) => check.versions[check.versions.length - 1];

  const versionFor = (check) => chosenVersion[check.id] ?? latestVersion(check)?.version;

  const versionEntry = (check) => check.versions.find((v) => v.version === versionFor(check));

  const queryFor = (check) => versionEntry(check)?.query || '';

  const hashFor = (check) => versionEntry(check)?.hash || '';

  const toggleExpand = (check) => {
    expanded[check.id] = !expanded[check.id];
  };

  const toggleCheck = (check) => {
    selected[check.id] = !selected[check.id];
    if (selected[check.id] && !chosenVersion[check.id]) {
      chosenVersion[check.id] = latestVersion(check)?.version;
    }
  };

  const onVersionChange = (check, version) => {
    chosenVersion[check.id] = version;
    if (!isSelected(check)) {
      selected[check.id] = true;
    }
  };

  const selectedItems = computed(() =>
    availableChecks.value
      .filter((c) => isSelected(c))
      .map((c) => ({ name: c.name, version: versionFor(c), hash: hashFor(c) }))
  );

  const selectedHashes = computed(() => selectedItems.value.map((item) => item.hash));

  const openQueryModal = (check, version) => {
    const entry = check.versions.find((v) => v.version === version);
    fullQuery.value = entry?.query || '';
    queryModalCheck.value = check;
    queryModalVersion.value = version;
    showQueryModal.value = true;
  };

  const loadData = async () => {
    loadingChecks.value = true;
    checkError.value = null;
    try {
      const [manifestData, checksData] = await Promise.all([
        apiService.getManifest(manifestId.value),
        apiService.getQualityChecksDetailed(),
      ]);
      manifest.value = manifestData;
      qualityChecks.value = checksData;
    } catch (err) {
      checkError.value = err.message || 'Failed to load data';
      console.error('Error loading publish data:', err);
    } finally {
      loadingChecks.value = false;
    }
  };

  const requestPublish = () => {
    if (selectedHashes.value.length === 0 || publishing.value) return;
    pendingItems.value = selectedItems.value;
    showConfirm.value = true;
  };

  const cancelPublish = () => {
    if (publishing.value) return;
    showConfirm.value = false;
    pendingItems.value = [];
  };

  const confirmPublish = async () => {
    if (pendingItems.value.length === 0 || publishing.value) return;

    publishing.value = true;
    try {
      const created = await apiService.createManifestVersion(manifestId.value, {
        hashes: pendingItems.value.map((item) => item.hash),
      });
      showConfirm.value = false;
      notificationService.success(
        'Version Published',
        `Version ${created.version} was published successfully`
      );
      router.push(`/manifests/${manifestId.value}`);
    } catch (err) {
      console.error('Error publishing version:', err);
      notificationService.error('Publish Failed', err.message || 'Failed to publish version');
    } finally {
      publishing.value = false;
    }
  };

  const goBack = () => {
    router.push(`/manifests/${manifestId.value}`);
  };

  onMounted(loadData);
</script>

<style scoped>
  .loading-state {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 4rem 0;
  }

  .font-monospace {
    font-family: var(--font-mono), monospace;
    font-size: 0.875rem;
  }

  .bg-light {
    background-color: #f8f9fa !important;
  }

  .expand-btn {
    line-height: 1;
  }

  .query-cell {
    max-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    vertical-align: middle;
  }

  .query-preview {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .versions-panel {
    background: #f8f9fa;
    padding: 0.75rem 1rem;
    border-left: 3px solid var(--color-primary, #0d6efd);
    margin: 0.25rem 1rem 0.75rem 2.5rem;
    border-radius: 0 0.5rem 0.5rem 0;
  }

  .version-item {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.35rem 0;
    flex-wrap: nowrap;
    min-width: 0;
  }

  .version-item .query-preview {
    flex: 1;
    min-width: 0;
  }

  .version-item.active .query-preview {
    font-weight: 600;
    color: #212529;
  }

  .query-full {
    max-height: 40vh;
    overflow: auto;
    white-space: pre-wrap;
    word-break: break-word;
  }

  .hashes-list {
    max-height: 40vh;
    overflow-y: auto;
    border: 1px solid #e5e7eb;
    border-radius: 0.5rem;
  }

  .hash-row {
    padding: 0.5rem 0.75rem;
  }

  .hash-row:not(:last-child) {
    border-bottom: 1px solid #f1f3f5;
  }

  .hash-row-info {
    display: flex;
    align-items: center;
    gap: 0.25rem;
    flex-wrap: wrap;
  }

  .hash-value {
    display: block;
    margin-top: 0.25rem;
    white-space: normal;
    word-break: break-all;
    overflow-wrap: anywhere;
    text-align: left;
  }
</style>
