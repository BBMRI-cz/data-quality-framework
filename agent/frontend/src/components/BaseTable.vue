<template>
  <div class="base-table">
    <div class="base-table__header">
      <h5 class="base-table__title">{{ title }}</h5>
      <span class="base-table__count">{{ items.length }} {{ itemLabel }}</span>
    </div>
    <div class="base-table__body">
      <table>
        <thead>
          <tr>
            <th v-for="col in columns" :key="col.key" :class="col.headerClass">
              {{ col.label }}
            </th>
          </tr>
        </thead>
        <tbody v-if="loading">
          <tr>
            <td :colspan="columns.length" class="base-table__loading">
              <div class="spinner"></div>
              <p>Loading...</p>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr v-for="item in items" :key="item[itemKey]" @click="$emit('row-click', item)">
            <td v-for="col in columns" :key="col.key" :class="getCellClass(col, item)">
              <slot :name="col.key" :item="item" :value="item[col.key]">
                {{ formatValue(item[col.key], col) }}
              </slot>
            </td>
          </tr>
          <tr v-if="showEmptyState">
            <td :colspan="columns.length" class="base-table__empty">
              <i :class="emptyIcon"></i>
              <p>{{ emptyText }}</p>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-if="totalPages > 1" class="base-table__pagination">
      <button
        class="pagination-btn"
        :disabled="currentPage === 0"
        @click="$emit('page-change', currentPage - 1)"
      >
        <i class="bi bi-chevron-left"></i>
      </button>
      <span class="pagination-info"> Page {{ currentPage + 1 }} of {{ totalPages }} </span>
      <button
        class="pagination-btn"
        :disabled="currentPage >= totalPages - 1"
        @click="$emit('page-change', currentPage + 1)"
      >
        <i class="bi bi-chevron-right"></i>
      </button>
    </div>
  </div>
</template>

<script setup>
  import { computed } from 'vue';

  const props = defineProps({
    title: {
      type: String,
      required: true,
    },
    columns: {
      type: Array,
      required: true,
    },
    items: {
      type: Array,
      default: () => [],
    },
    itemKey: {
      type: String,
      default: 'id',
    },
    totalElements: {
      type: Number,
      default: 0,
    },
    totalPages: {
      type: Number,
      default: 1,
    },
    currentPage: {
      type: Number,
      default: 0,
    },
    itemLabel: {
      type: String,
      default: 'items',
    },
    emptyText: {
      type: String,
      default: 'No items available',
    },
    emptyIcon: {
      type: String,
      default: 'bi bi-inbox',
    },
    loading: {
      type: Boolean,
      default: true,
    },
  });

  defineEmits(['row-click', 'page-change']);

  // Show empty state only when not loading and no items
  const showEmptyState = computed(() => {
    return props.items.length === 0;
  });

  const formatValue = (value, col) => {
    if (value == null) return col.fallback || 'N/A';
    if (col.format === 'decimal') return value.toFixed(2);
    return value;
  };

  const getCellClass = (col, item) => {
    if (typeof col.cellClass === 'function') {
      return col.cellClass(item);
    }
    return col.cellClass;
  };
</script>

<style scoped>
  .base-table {
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
    overflow: hidden;
  }

  .base-table__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--spacing-md) var(--spacing-lg);
    border-bottom: 1px solid var(--color-gray-100);
  }

  .base-table__title {
    font-size: 1rem;
    font-weight: 600;
    margin: 0;
    color: var(--color-gray-800);
  }

  .base-table__count {
    font-size: 0.75rem;
    font-weight: 500;
    color: var(--color-gray-500);
    background: var(--color-gray-100);
    padding: 0.25rem 0.75rem;
    border-radius: var(--radius-full);
  }

  .base-table__body {
    overflow-x: auto;
  }

  table {
    width: 100%;
    border-collapse: collapse;
  }

  th {
    font-weight: 600;
    font-size: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--color-gray-500);
    padding: var(--spacing-md) var(--spacing-md);
    border-bottom: 2px solid var(--color-gray-200);
    background-color: var(--color-gray-50);
    text-align: left;
  }

  th.center {
    text-align: center;
  }

  td {
    padding: var(--spacing-lg) var(--spacing-md);
    border-bottom: 1px solid var(--color-gray-100);
    color: var(--color-gray-600);
  }

  td.center {
    text-align: center;
    font-variant-numeric: tabular-nums;
    font-weight: 500;
  }

  td.warning {
    color: #ca8a04;
  }

  td.danger {
    color: var(--color-danger);
  }

  td.success {
    color: var(--color-success);
  }

  td.truncate {
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  tbody tr {
    cursor: pointer;
    transition: background-color var(--transition-base);
  }

  tbody tr:hover {
    background-color: var(--bg-hover);
  }

  .base-table__empty {
    text-align: center;
    padding: var(--spacing-3xl) var(--spacing-lg);
    color: var(--color-gray-400);
  }

  .base-table__empty i {
    font-size: 3rem;
    display: block;
    margin-bottom: var(--spacing-md);
    opacity: 0.5;
  }

  .base-table__empty p {
    margin: 0;
  }

  .base-table__loading {
    text-align: center;
    padding: var(--spacing-3xl) var(--spacing-lg);
    color: var(--color-gray-500);
  }

  .base-table__loading div,
  .base-table__loading p {
    display: inline-block;
  }

  .base-table__loading .spinner {
    display: block;
    margin: 0 auto;
  }

  .base-table__loading p {
    display: block;
    margin: var(--spacing-md) 0 0;
    font-size: 0.875rem;
  }

  .spinner {
    width: 2.5rem;
    height: 2.5rem;
    border: 3px solid var(--color-gray-200);
    border-top-color: var(--color-primary, #3b82f6);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }

  .base-table__pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: var(--spacing-md);
    padding: var(--spacing-md) var(--spacing-lg);
    border-top: 1px solid var(--color-gray-100);
  }

  .pagination-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 2rem;
    height: 2rem;
    border: 1px solid var(--color-gray-200);
    border-radius: var(--radius-md);
    background: var(--bg-card);
    color: var(--color-gray-600);
    cursor: pointer;
    transition: all var(--transition-base);
  }

  .pagination-btn:hover:not(:disabled) {
    background: var(--color-gray-100);
    color: var(--color-gray-800);
  }

  .pagination-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .pagination-info {
    font-size: 0.875rem;
    color: var(--color-gray-600);
  }

  /* Responsive column hiding */
  @media (max-width: 1200px) {
    th.hide-xl,
    td.hide-xl {
      display: none;
    }
  }

  @media (max-width: 992px) {
    th.hide-lg,
    td.hide-lg {
      display: none;
    }
  }

  @media (max-width: 768px) {
    th.hide-md,
    td.hide-md {
      display: none;
    }

    th,
    td {
      padding: var(--spacing-md) var(--spacing-sm);
    }
  }

  @media (max-width: 576px) {
    th.hide-sm,
    td.hide-sm {
      display: none;
    }
  }
</style>
