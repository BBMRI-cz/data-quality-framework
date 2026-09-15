<template>
  <div class="card border-0 shadow-sm mb-4">
    <div class="card-header bg-white border-bottom py-3">
      <div class="d-flex flex-wrap align-items-center justify-content-between gap-2">
        <h5 class="mb-0 fw-semibold">
          <i class="bi bi-clock-history text-primary me-2"></i>
          Versions
        </h5>
        <div v-if="versions.length > 0" class="d-flex align-items-center gap-2">
          <div class="btn-group" role="group" aria-label="Version selector">
            <button
              v-for="version in sortedVersions"
              :key="version.id"
              type="button"
              class="btn btn-sm"
              :class="isCurrentVersion(version) ? 'btn-primary' : 'btn-outline-primary'"
              @click="selectVersion(version)"
            >
              v{{ version.version }}
            </button>
          </div>
          <button
            type="button"
            class="btn btn-sm btn-outline-secondary"
            title="View signed manifest body"
            @click="openBodyModal(currentVersion)"
          >
            <i class="bi bi-file-earmark-lock me-1"></i>Signed Body
          </button>
        </div>
      </div>
    </div>

    <div class="card-body p-4">
      <div v-if="sortedVersions.length === 0" class="text-muted">
        <i class="bi bi-inbox me-1"></i>No versions published yet
      </div>

      <template v-else>
        <div v-if="currentVersion" class="d-flex flex-wrap align-items-center gap-3 mb-3">
          <span class="badge bg-primary">v{{ currentVersion.version }}</span>
          <span class="text-muted small">
            <i class="bi bi-calendar-event me-1"></i>{{ formatDate(currentVersion.generatedAt) }}
          </span>
          <span v-if="isSigned(currentVersion)" class="text-success small">
            <i class="bi bi-check-circle-fill me-1"></i>Signed
          </span>
          <span v-else class="text-muted small">
            <i class="bi bi-dash-circle me-1"></i>Unsigned
          </span>
        </div>

        <h6 class="fw-semibold mb-2">
          <i class="bi bi-check2-square me-1"></i>Linked Quality Checks
        </h6>

        <div v-if="loadingChecks" class="text-muted py-2">
          <span class="spinner-border spinner-border-sm me-2" role="status"></span>Loading quality
          checks...
        </div>
        <div v-else-if="versionQualityChecks.length === 0" class="text-muted">
          <i class="bi bi-dash-circle me-1"></i>No linked quality checks
        </div>
        <div v-else class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead>
              <tr>
                <th>Quality Check</th>
                <th style="width: 6rem">Version</th>
                <th>Query</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="check in versionQualityChecks"
                :key="check.id"
                class="check-row"
                title="Open quality check"
                @click="goToQualityCheck(check)"
              >
                <td>
                  <span class="fw-medium text-primary check-name">{{ check.name }}</span>
                  <div v-if="check.description" class="text-muted small">
                    {{ truncate(check.description) }}
                  </div>
                </td>
                <td>
                  <span class="badge bg-primary">v{{ check.versions?.[0]?.version }}</span>
                </td>
                <td>
                  <code class="font-monospace small text-muted">{{
                    truncate(check.versions?.[0]?.query)
                  }}</code>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </div>

    <BaseModal
      :show="!!bodyVersion"
      title="Signed Manifest Body"
      icon="bi bi-file-earmark-lock"
      size="lg"
      :show-footer="false"
      @close="bodyVersion = null"
    >
      <div v-if="bodyVersion">
        <div class="d-flex align-items-center gap-3 flex-wrap mb-3">
          <span class="badge bg-primary">v{{ bodyVersion.version }}</span>
          <span class="text-muted small">
            <i class="bi bi-calendar-event me-1"></i>{{ formatDate(bodyVersion.generatedAt) }}
          </span>
          <span v-if="isSigned(bodyVersion)" class="text-success small">
            <i class="bi bi-check-circle-fill me-1"></i>Signed
          </span>
          <span v-else class="text-muted small">
            <i class="bi bi-dash-circle me-1"></i>Unsigned
          </span>
        </div>

        <dl class="row mb-3">
          <template v-if="bodyVersion.keyId">
            <dt class="col-sm-3">Key ID</dt>
            <dd class="col-sm-9">
              <code class="font-monospace small">{{ bodyVersion.keyId }}</code>
            </dd>
          </template>
          <template v-if="bodyVersion.signature">
            <dt class="col-sm-3">Signature</dt>
            <dd class="col-sm-9">
              <code class="font-monospace small text-break">{{ bodyVersion.signature }}</code>
            </dd>
          </template>
        </dl>

        <h6 class="fw-semibold mb-2"><i class="bi bi-code-slash me-1"></i>Manifest Body</h6>
        <pre
          v-if="bodyJson(bodyVersion)"
          class="query-full font-monospace bg-light rounded p-3 mb-0"
          >{{ bodyJson(bodyVersion) }}</pre
        >
        <div v-else class="text-muted"><i class="bi bi-dash-circle me-1"></i>No body stored</div>
      </div>
    </BaseModal>
  </div>
</template>

<script setup>
  import { ref, computed, watch } from 'vue';
  import { useRouter } from 'vue-router';
  import { formatDateLong } from '@/utils/dateUtils.js';
  import BaseModal from './BaseModal.vue';
  import { apiService } from '@/services/apiService.js';

  const props = defineProps({
    versions: {
      type: Array,
      default: () => [],
    },
    manifestId: {
      type: [Number, String],
      required: true,
    },
  });

  const router = useRouter();

  const selectedVersion = ref(null);
  const versionQualityChecks = ref([]);
  const loadingChecks = ref(false);
  const bodyVersion = ref(null);
  const loadingVersionId = ref(null);

  const sortedVersions = computed(() => [...props.versions].sort((a, b) => a.version - b.version));

  const currentVersion = computed(
    () => selectedVersion.value || sortedVersions.value[sortedVersions.value.length - 1] || null
  );

  const formatDate = formatDateLong;

  const isSigned = (version) => !!version && !!version.signature;

  const isCurrentVersion = (version) => currentVersion.value?.id === version.id;

  const truncate = (text, length = 40) => {
    if (!text) return '';
    return text.length > length ? text.slice(0, length).trimEnd() + '…' : text;
  };

  const bodyJson = (version) => {
    if (!version || version.body === null || version.body === undefined) return null;
    try {
      const body = typeof version.body === 'string' ? JSON.parse(version.body) : version.body;
      return JSON.stringify(body, null, 2);
    } catch {
      return typeof version.body === 'string' ? version.body : null;
    }
  };

  const openBodyModal = (version) => {
    if (!version) return;
    bodyVersion.value = version;
  };

  const selectVersion = (version) => {
    selectedVersion.value = version;
    loadQualityChecks(version);
  };

  const loadQualityChecks = async (version) => {
    if (!version) return;
    loadingVersionId.value = version.id;
    loadingChecks.value = true;
    try {
      const data = await apiService.getManifestVersionQualityChecks(props.manifestId, version.id);
      versionQualityChecks.value =
        data?._embedded?.qualityChecks || (Array.isArray(data) ? data : []);
    } catch (err) {
      versionQualityChecks.value = [];
      console.error('Error loading linked quality checks:', err);
    } finally {
      loadingChecks.value = false;
      loadingVersionId.value = null;
    }
  };

  const goToQualityCheck = (check) => {
    if (check?.id) {
      router.push(`/quality-checks/${check.id}`);
    }
  };

  watch(
    () => props.versions,
    (versions) => {
      const latest = versions[versions.length - 1] || null;
      selectedVersion.value = latest;
      loadQualityChecks(latest);
    },
    { immediate: true }
  );
</script>

<style scoped>
  .font-monospace {
    font-family: var(--font-mono), monospace;
    font-size: 0.875rem;
  }

  .bg-light {
    background-color: #f8f9fa !important;
  }

  .check-row {
    cursor: pointer;
  }

  .check-name:hover {
    text-decoration: underline;
  }

  .query-full {
    max-height: 50vh;
    overflow: auto;
    white-space: pre-wrap;
    word-break: break-word;
  }
</style>
