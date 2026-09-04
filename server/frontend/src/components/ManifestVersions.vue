<template>
  <div class="card border-0 shadow-sm mb-4">
    <div class="card-header bg-white border-bottom py-3">
      <h5 class="mb-0 fw-semibold">
        <i class="bi bi-clock-history text-primary me-2"></i>
        Versions
      </h5>
    </div>
    <div class="card-body p-4">
      <div v-if="sortedVersions.length === 0" class="text-muted">
        <i class="bi bi-inbox me-1"></i>No versions published yet
      </div>
      <div v-else class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead>
            <tr>
              <th style="width: 6rem">Version</th>
              <th>Generated</th>
              <th style="width: 6rem">Signed</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="version in sortedVersions"
              :key="version.id"
              class="version-row"
              @click="openVersionModal(version)"
            >
              <td>
                <span class="badge bg-primary">v{{ version.version }}</span>
              </td>
              <td>
                <span class="text-muted">
                  <i class="bi bi-calendar-event me-1"></i>{{ formatDate(version.generatedAt) }}
                </span>
              </td>
              <td>
                <span v-if="isSigned(version)" class="text-success" title="Signed">
                  <i class="bi bi-check-circle-fill"></i>
                </span>
                <span v-else class="text-muted" title="Unsigned">
                  <i class="bi bi-dash-circle"></i>
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <BaseModal
      :show="!!selectedVersion"
      title="Manifest Version"
      icon="bi bi-file-earmark-lock"
      size="lg"
      :show-footer="false"
      @close="selectedVersion = null"
    >
      <div v-if="selectedVersion">
        <div class="d-flex align-items-center gap-3 flex-wrap mb-3">
          <span class="badge bg-primary">v{{ selectedVersion.version }}</span>
          <span class="text-muted small">
            <i class="bi bi-calendar-event me-1"></i>{{ formatDate(selectedVersion.generatedAt) }}
          </span>
          <span v-if="isSigned(selectedVersion)" class="text-success small">
            <i class="bi bi-check-circle-fill me-1"></i>Signed
          </span>
          <span v-else class="text-muted small">
            <i class="bi bi-dash-circle me-1"></i>Unsigned
          </span>
        </div>

        <dl class="row mb-3">
          <template v-if="selectedVersion.keyId">
            <dt class="col-sm-3">Key ID</dt>
            <dd class="col-sm-9">
              <code class="font-monospace small">{{ selectedVersion.keyId }}</code>
            </dd>
          </template>
          <template v-if="selectedVersion.signature">
            <dt class="col-sm-3">Signature</dt>
            <dd class="col-sm-9">
              <code class="font-monospace small text-break">{{ selectedVersion.signature }}</code>
            </dd>
          </template>
        </dl>

        <h6 class="fw-semibold mb-2"><i class="bi bi-code-slash me-1"></i>Manifest Body</h6>
        <pre
          v-if="bodyJson(selectedVersion)"
          class="query-full font-monospace bg-light rounded p-3 mb-0"
          >{{ bodyJson(selectedVersion) }}</pre
        >
        <div v-else class="text-muted"><i class="bi bi-dash-circle me-1"></i>No body stored</div>
      </div>
    </BaseModal>
  </div>
</template>

<script setup>
  import { ref, computed } from 'vue';
  import { formatDateLong } from '@/utils/dateUtils.js';
  import BaseModal from './BaseModal.vue';

  const props = defineProps({
    versions: {
      type: Array,
      default: () => [],
    },
  });

  const selectedVersion = ref(null);

  const sortedVersions = computed(() => [...props.versions].sort((a, b) => a.version - b.version));

  const formatDate = formatDateLong;

  const isSigned = (version) => !!version && !!version.signature;

  const bodyJson = (version) => {
    if (!version || version.body === null || version.body === undefined) return null;
    try {
      const body = typeof version.body === 'string' ? JSON.parse(version.body) : version.body;
      return JSON.stringify(body, null, 2);
    } catch {
      return typeof version.body === 'string' ? version.body : null;
    }
  };

  const openVersionModal = (version) => {
    selectedVersion.value = version;
  };
</script>

<style scoped>
  .font-monospace {
    font-family: var(--font-mono), monospace;
    font-size: 0.875rem;
  }

  .bg-light {
    background-color: #f8f9fa !important;
  }

  .version-row {
    cursor: pointer;
  }

  .query-full {
    max-height: 50vh;
    overflow: auto;
    white-space: pre-wrap;
    word-break: break-word;
  }
</style>
