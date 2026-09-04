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
        <i class="bi bi-inbox me-1"></i>No versions yet
      </div>
      <ul v-else class="list-group list-group-flush">
        <li v-for="version in sortedVersions" :key="version.id" class="list-group-item px-0">
          <div class="d-flex justify-content-between align-items-center">
            <span>
              <span class="badge bg-primary">v{{ version.version }}</span>
              <span class="badge bg-secondary ms-1">{{ formatQueryType(version.type) }}</span>
            </span>
            <span class="text-muted small font-monospace">{{ version.hash }}</span>
          </div>
          <pre v-if="version.query" class="mt-2 mb-0 p-2 bg-light rounded font-monospace small">{{
            version.query
          }}</pre>
          <div v-else class="mt-2 text-muted small">
            <i class="bi bi-dash-circle me-1"></i>No query body stored
          </div>
        </li>
      </ul>

      <hr class="my-4" />

      <h6 class="fw-semibold mb-3">
        <i class="bi bi-plus-circle me-1"></i>
        Add New Version
      </h6>
      <div class="mb-3">
        <label for="new-version-type" class="form-label">Query Type</label>
        <select
          id="new-version-type"
          v-model="newVersionType"
          class="form-select"
          :disabled="savingVersion"
        >
          <option v-for="type in QUERY_TYPES" :key="type" :value="type">
            {{ formatQueryType(type) }}
          </option>
        </select>
      </div>
      <div class="mb-3">
        <textarea
          v-model="newVersionQuery"
          class="form-control font-monospace"
          rows="4"
          placeholder="Enter the query for the new version..."
          :disabled="savingVersion"
        ></textarea>
      </div>
      <button
        class="btn btn-primary"
        :disabled="!newVersionQuery.trim() || savingVersion"
        @click="requestAddVersion"
      >
        <span
          v-if="savingVersion"
          class="spinner-border spinner-border-sm me-2"
          role="status"
        ></span>
        <i v-else class="bi bi-plus-lg me-2"></i>
        Add Version
      </button>
    </div>

    <ConfirmModal
      :show="showConfirm"
      title="Add New Version"
      subtitle="Confirm version creation"
      message="You are about to create a new version of this quality check:"
      confirm-text="Yes, Add Version"
      :loading="savingVersion"
      @close="cancelAddVersion"
      @confirm="confirmAddVersion"
    >
      <template #body>
        <div class="py-2">
          <p class="mb-2">
            <span class="text-muted small me-1">Query Type:</span>
            <span class="badge bg-secondary">{{ formatQueryType(pendingType) }}</span>
          </p>
          <pre class="mb-0 mx-auto p-2 bg-light rounded font-monospace small text-start">{{
            pendingQuery
          }}</pre>
          <p class="text-muted small mt-2 mb-0">
            <i class="bi bi-info-circle me-1"></i>
            The new version will be immutable once created.
          </p>
        </div>
      </template>
    </ConfirmModal>
  </div>
</template>

<script setup>
  import { ref, computed } from 'vue';
  import { apiService } from '@/services/apiService.js';
  import { notificationService } from '@/services/notificationService.js';
  import { QUERY_TYPES, formatQueryType } from '@/utils/queryTypeUtils.js';
  import ConfirmModal from './ConfirmModal.vue';

  const props = defineProps({
    versions: {
      type: Array,
      default: () => [],
    },
    checkId: {
      type: [Number, String],
      required: true,
    },
  });

  const emit = defineEmits(['version-added']);

  const newVersionQuery = ref('');
  const newVersionType = ref('UNKNOWN');
  const savingVersion = ref(false);
  const showConfirm = ref(false);
  const pendingQuery = ref('');
  const pendingType = ref('UNKNOWN');

  const sortedVersions = computed(() => [...props.versions].sort((a, b) => a.version - b.version));

  const requestAddVersion = () => {
    const query = newVersionQuery.value.trim();
    if (!query || savingVersion.value) return;
    pendingQuery.value = query;
    pendingType.value = newVersionType.value;
    showConfirm.value = true;
  };

  const cancelAddVersion = () => {
    if (savingVersion.value) return;
    showConfirm.value = false;
    pendingQuery.value = '';
  };

  const confirmAddVersion = async () => {
    if (!pendingQuery.value || savingVersion.value) return;

    savingVersion.value = true;
    try {
      const created = await apiService.createQualityCheckVersion(
        props.checkId,
        pendingQuery.value,
        pendingType.value
      );
      newVersionQuery.value = '';
      newVersionType.value = 'UNKNOWN';
      pendingQuery.value = '';
      showConfirm.value = false;
      emit('version-added', created);
      notificationService.success(
        'Version Added',
        `Version ${created.version} created successfully`
      );
    } catch (err) {
      console.error('Error adding version:', err);
      notificationService.error('Add Version Failed', err.message || 'Failed to add version');
    } finally {
      savingVersion.value = false;
    }
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
</style>
