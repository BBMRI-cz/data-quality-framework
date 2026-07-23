<template>
  <div>
    <!-- Error state -->
    <div v-if="error" class="error-state">
      <h6>Error Loading Quality Checks</h6>
      <p>{{ error }}</p>
    </div>

    <!-- Quality Checks Table -->
    <BaseTable
      v-else
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
      <template #name="{ item }">
        <i class="bi bi-check-square icon"></i>
        {{ item.name }}
      </template>
      <template #category="{ item }">
        <CategoryBadge :category="item.category" />
      </template>
      <template #query="{ value }">
        {{ truncateText(value, 30) }}
      </template>
    </BaseTable>
  </div>
</template>

<script setup>
  import { computed } from 'vue';
  import { useRouter } from 'vue-router';
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
    pagination: {
      type: Object,
      default: () => ({
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
      }),
    },
  });

  const emit = defineEmits(['page-change']);

  const router = useRouter();

  const displayedChecks = computed(() => props.items);

  const columns = [
    { key: 'name', label: 'Name' },
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
