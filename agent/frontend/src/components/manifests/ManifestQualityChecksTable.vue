<template>
  <div>
    <!-- Error state -->
    <div v-if="error" class="alert alert-danger mb-4">
      <i class="bi bi-exclamation-triangle me-2"></i>
      {{ error }}
    </div>

    <!-- Quality Checks Table -->
    <BaseTable
      v-else
      title="Quality Checks"
      :columns="columns"
      :items="checks"
      :loading="loading"
      item-key="id"
      item-label="checks"
      empty-text="No quality checks pinned by this version"
      empty-icon="bi bi-check-square"
      @row-click="openCheck"
    >
      <template #name="{ item }">
        <div class="d-flex flex-column gap-1">
          <span class="fw-medium">{{ item.name }}</span>
          <span v-if="item.description" class="text-muted small">
            {{ truncateText(item.description, 80) }}
          </span>
        </div>
      </template>
      <template #category="{ item }">
        <CategoryBadge :category="item.category" />
      </template>
      <template #type="{ item }">
        <span v-if="item.type" class="badge bg-secondary">{{ item.type }}</span>
        <span v-else class="text-muted">—</span>
      </template>
      <template #query="{ item }">
        <code class="query-preview">{{ truncateText(item.query, 40) }}</code>
      </template>
      <template #thresholds="{ item }">
        <span class="text-warning fw-semibold">{{ item.warningThreshold }}%</span>
        <span class="text-muted mx-1">/</span>
        <span class="text-danger fw-semibold">{{ item.errorThreshold }}%</span>
      </template>
    </BaseTable>

    <!-- Quality Check Detail Modal -->
    <BaseModal
      :show="showCheckModal"
      :title="selectedCheck?.name || 'Quality Check'"
      size="lg"
      :show-footer="false"
      @close="closeCheck"
    >
      <template v-if="selectedCheck">
        <div class="check-meta mb-3">
          <div class="meta-item">
            <span class="meta-label">Category</span>
            <span><CategoryBadge :category="selectedCheck.category" /></span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Type</span>
            <span>
              <span v-if="selectedCheck.type" class="badge bg-secondary">{{
                selectedCheck.type
              }}</span>
              <span v-else class="text-muted">N/A</span>
            </span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Warning Threshold</span>
            <span class="text-warning fw-semibold">{{ selectedCheck.warningThreshold }}%</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Error Threshold</span>
            <span class="text-danger fw-semibold">{{ selectedCheck.errorThreshold }}%</span>
          </div>
        </div>
        <div v-if="selectedCheck.description" class="meta-item mb-3">
          <span class="meta-label">Description</span>
          <span>{{ selectedCheck.description }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">Query</span>
          <pre class="query-viewer">{{ selectedCheck.query || 'N/A' }}</pre>
        </div>
      </template>
    </BaseModal>
  </div>
</template>

<script setup>
  import { ref } from 'vue';
  import BaseModal from '@/components/BaseModal.vue';
  import BaseTable from '@/components/BaseTable.vue';
  import CategoryBadge from '@/components/CategoryBadge.vue';
  import { truncateText } from '@/utils/stringUtils.js';

  defineProps({
    checks: {
      type: Array,
      default: () => [],
    },
    loading: {
      type: Boolean,
      default: false,
    },
    error: {
      type: String,
      default: null,
    },
  });

  const columns = [
    { key: 'name', label: 'Name' },
    { key: 'category', label: 'Category' },
    { key: 'type', label: 'Type', headerClass: 'center', cellClass: 'center' },
    { key: 'query', label: 'Query' },
    { key: 'thresholds', label: 'Warn / Error', headerClass: 'center', cellClass: 'center' },
  ];

  const showCheckModal = ref(false);
  const selectedCheck = ref(null);

  function openCheck(check) {
    selectedCheck.value = check;
    showCheckModal.value = true;
  }

  function closeCheck() {
    showCheckModal.value = false;
    selectedCheck.value = null;
  }
</script>

<style scoped>
  .query-preview {
    font-family: var(--font-mono), monospace;
    font-size: 0.8rem;
    color: var(--color-gray-600);
  }

  .check-meta {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: var(--spacing-md);
  }

  .meta-item {
    display: flex;
    flex-direction: column;
    gap: 2px;
    align-items: flex-start;
  }

  .meta-label {
    font-size: 0.75rem;
    font-weight: 600;
    color: var(--color-gray-500);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .query-viewer {
    width: 100%;
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

  .alert-danger {
    background-color: rgba(239, 68, 68, 0.1);
    color: #991b1b;
  }
</style>
