<template>
  <div>
    <!-- Error state -->
    <div v-if="error" class="error-state">
      <h6>Error Loading Quality Checks</h6>
      <p>{{ error }}</p>
    </div>

    <template v-else>
      <!-- Bulk actions bar -->
      <div v-if="selectedIds.length > 0" class="bulk-actions">
        <span class="bulk-actions__count">{{ selectedIds.length }} selected</span>
        <ActionButton
          icon="bi bi-check-lg"
          text="Activate"
          variant="primary"
          :disabled="updating"
          @click="emitBulkUpdate(true)"
        />
        <ActionButton
          icon="bi bi-x-lg"
          text="Deactivate"
          variant="secondary"
          :disabled="updating"
          @click="emitBulkUpdate(false)"
        />
        <button class="bulk-actions__clear" :disabled="updating" @click="clearSelection">
          Clear
        </button>
      </div>

      <!-- Quality Checks Table -->
      <BaseTable
        title="Quality Checks"
        :loading="loading"
        :columns="columns"
        :items="displayedChecks"
        :total-elements="pagination.totalElements"
        :total-pages="pagination.totalPages"
        :current-page="pagination.page"
        item-label="checks"
        empty-text="No quality checks configured yet"
        empty-icon="bi bi-check-square"
        @row-click="navigateToEdit"
        @page-change="handlePageChange"
      >
        <template #header-select>
          <input
            type="checkbox"
            class="form-check-input"
            aria-label="Select all quality checks"
            :checked="allSelected"
            :disabled="displayedChecks.length === 0"
            @click.stop
            @change="toggleAll"
          />
        </template>
        <template #select="{ item }">
          <input
            type="checkbox"
            class="form-check-input"
            aria-label="Select quality check"
            :checked="selectedIds.includes(item.id)"
            @click.stop
            @change="toggleSelect(item.id)"
          />
        </template>
        <template #name="{ item }">
          <i class="bi bi-check-square icon"></i>
          {{ item.name }}
        </template>
        <template #active="{ item }">
          <span
            class="status-badge"
            :class="item.active ? 'status-badge--active' : 'status-badge--inactive'"
          >
            <i class="bi" :class="item.active ? 'bi-check-circle-fill' : 'bi-pause-circle'"></i>
            {{ item.active ? 'Active' : 'Inactive' }}
          </span>
        </template>
        <template #category="{ item }">
          <CategoryBadge :category="item.category" />
        </template>
        <template #query="{ value }">
          {{ truncateText(value, 30) }}
        </template>
      </BaseTable>
    </template>
  </div>
</template>

<script setup>
  import { computed, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import ActionButton from '@/components/ActionButton.vue';
  import BaseTable from '@/components/BaseTable.vue';
  import CategoryBadge from '@/components/CategoryBadge.vue';
  import { truncateText } from '@/utils/stringUtils.js';

  const props = defineProps({
    items: {
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
    updating: {
      type: Boolean,
      default: false,
    },
    pagination: {
      type: Object,
      default: () => ({
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
      }),
    },
    fetchAllIds: {
      type: Function,
      default: null,
    },
  });

  const emit = defineEmits(['page-change', 'bulk-update']);

  const router = useRouter();

  const displayedChecks = computed(() => props.items);

  const selectedIds = ref([]);

  const allSelected = computed(
    () =>
      displayedChecks.value.length > 0 &&
      displayedChecks.value.every((item) => selectedIds.value.includes(item.id))
  );

  const toggleSelect = (id) => {
    if (selectedIds.value.includes(id)) {
      selectedIds.value = selectedIds.value.filter((selectedId) => selectedId !== id);
    } else {
      selectedIds.value = [...selectedIds.value, id];
    }
  };

  const toggleAll = async () => {
    if (allSelected.value) {
      selectedIds.value = [];
      return;
    }
    if (props.fetchAllIds) {
      // Select all quality checks across all pages, not just the currently displayed page
      const ids = await props.fetchAllIds();
      if (ids !== null) {
        selectedIds.value = ids;
      }
    } else {
      selectedIds.value = displayedChecks.value.map((item) => item.id);
    }
  };

  const clearSelection = () => {
    selectedIds.value = [];
  };

  const emitBulkUpdate = (active) => {
    emit('bulk-update', { ids: [...selectedIds.value], active });
  };

  defineExpose({ clearSelection });

  const columns = [
    { key: 'select', label: '', headerClass: 'center', cellClass: 'center' },
    { key: 'name', label: 'Name' },
    {
      key: 'active',
      label: 'Status',
      headerClass: 'center',
      cellClass: 'center',
    },
    {
      key: 'category',
      label: 'Category',
      headerClass: 'hide-md',
      cellClass: 'hide-md',
    },
    {
      key: 'description',
      label: 'Description',
      headerClass: 'hide-md',
      cellClass: 'hide-md',
      fallback: 'No description',
    },
    { key: 'query', label: 'Query', headerClass: 'hide-lg', cellClass: 'hide-lg truncate' },
    {
      key: 'warningThreshold',
      label: 'Warning',
      headerClass: 'center hide-lg',
      cellClass: 'center warning hide-lg',
    },
    {
      key: 'errorThreshold',
      label: 'Error',
      headerClass: 'center hide-lg',
      cellClass: 'center danger hide-lg',
    },
    {
      key: 'epsilonBudget',
      label: 'Epsilon',
      headerClass: 'center hide-xl',
      cellClass: 'center hide-xl',
      format: 'decimal',
    },
  ];

  const navigateToEdit = (item) => {
    router.push(`/quality-checks/${item.id}/edit`);
  };

  const handlePageChange = (page) => {
    emit('page-change', page);
  };
</script>

<style scoped>
  .icon {
    color: var(--color-primary);
    margin-right: var(--spacing-sm);
  }

  .bulk-actions {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    flex-wrap: wrap;
    margin-bottom: var(--spacing-md);
    padding: var(--spacing-sm) var(--spacing-md);
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
  }

  .bulk-actions__count {
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--color-gray-600);
    margin-right: var(--spacing-sm);
  }

  .bulk-actions__clear {
    background: none;
    border: none;
    padding: var(--spacing-sm);
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--color-gray-500);
    cursor: pointer;
    transition: color var(--transition-base);
  }

  .bulk-actions__clear:hover:not(:disabled) {
    color: var(--color-gray-800);
  }

  .bulk-actions__clear:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .status-badge {
    display: inline-flex;
    align-items: center;
    gap: var(--spacing-xs);
    padding: 0.125rem 0.625rem;
    border-radius: var(--radius-full);
    border: 1px solid;
    font-size: 0.75rem;
    font-weight: 600;
  }

  .status-badge--active {
    color: var(--color-success);
    border-color: var(--color-success);
  }

  .status-badge--inactive {
    color: var(--color-gray-500);
    border-color: var(--color-gray-300);
  }

  .error-state {
    background: #fee2e2;
    border: 1px solid #fecaca;
    color: var(--color-danger);
    padding: var(--spacing-lg);
    border-radius: var(--radius-lg);
  }

  .error-state h6 {
    margin: 0 0 var(--spacing-sm);
    font-weight: 600;
  }

  .error-state p {
    margin: 0;
  }

  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
    color: var(--color-gray-400);
  }

  .empty-state i {
    font-size: 4rem;
    margin-bottom: var(--spacing-md);
  }

  .empty-state h5 {
    color: var(--color-gray-800);
    margin-bottom: var(--spacing-sm);
  }

  .empty-state p {
    margin: 0;
  }

  @media (max-width: 768px) {
    .filters-content {
      flex-wrap: wrap;
    }

    .results-count {
      order: 3;
      width: 100%;
    }
  }
</style>
