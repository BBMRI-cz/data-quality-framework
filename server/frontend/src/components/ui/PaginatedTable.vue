<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white border-bottom py-3">
      <div class="d-flex justify-content-between align-items-center gap-2">
        <h5 class="mb-0 fw-semibold">{{ title }}</h5>
        <slot name="header-meta">
          <span class="text-muted small">{{ totalItems }} {{ itemLabel }}</span>
        </slot>
      </div>
    </div>

    <div class="card-body p-0">
      <div class="table-responsive">
        <table class="table table-hover mb-0 align-middle">
          <thead class="table-light">
            <tr>
              <th v-for="column in columns" :key="column.key" :class="column.headerClass">
                {{ column.label }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(item, index) in paginatedItems"
              :key="resolveItemKey(item, index)"
              :class="{ 'table-row-hover cursor-pointer': rowClickable }"
              @click="handleRowClick(item)"
            >
              <td v-for="column in columns" :key="column.key" :class="column.cellClass">
                <slot :name="`cell-${column.key}`" :item="item" :value="item[column.key]">
                  {{ formatCellValue(item[column.key], column) }}
                </slot>
              </td>
            </tr>
            <tr v-if="paginatedItems.length === 0">
              <td :colspan="columns.length" class="text-center text-muted py-5">
                <p class="mb-0">{{ emptyText }}</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="showPagination" class="card-footer bg-white border-top py-2">
      <div class="d-flex justify-content-end align-items-center gap-2">
        <button
          type="button"
          class="btn btn-sm btn-outline-secondary"
          :disabled="page === 0"
          aria-label="Previous page"
          @click="changePage(page - 1)"
        >
          Previous
        </button>
        <span class="small text-muted">Page {{ page + 1 }} of {{ totalPages }}</span>
        <button
          type="button"
          class="btn btn-sm btn-outline-secondary"
          :disabled="page >= totalPages - 1"
          aria-label="Next page"
          @click="changePage(page + 1)"
        >
          Next
        </button>
      </div>
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
    page: {
      type: Number,
      default: 0,
    },
    pageSize: {
      type: Number,
      default: 10,
    },
    totalItems: {
      type: Number,
      default: undefined,
    },
    itemLabel: {
      type: String,
      default: 'items',
    },
    itemKey: {
      type: [String, Function],
      default: 'id',
    },
    rowClickable: {
      type: Boolean,
      default: true,
    },
    emptyText: {
      type: String,
      default: 'No data available',
    },
    paginate: {
      type: Boolean,
      default: true,
    },
  });

  const emit = defineEmits(['row-click', 'page-change']);

  const totalItems = computed(() =>
    typeof props.totalItems === 'number' ? props.totalItems : props.items.length
  );

  const totalPages = computed(() => {
    const size = Math.max(props.pageSize, 1);
    const pages = Math.ceil(totalItems.value / size);
    return pages > 0 ? pages : 1;
  });

  const showPagination = computed(() => props.paginate && totalPages.value > 1);

  const paginatedItems = computed(() => {
    if (!props.paginate) {
      return props.items;
    }

    const size = Math.max(props.pageSize, 1);
    const start = Math.max(props.page, 0) * size;
    return props.items.slice(start, start + size);
  });

  const resolveItemKey = (item, index) => {
    if (typeof props.itemKey === 'function') {
      return props.itemKey(item);
    }
    return item?.[props.itemKey] ?? index;
  };

  const formatCellValue = (value, column) => {
    return value == null ? column.fallback || 'N/A' : value;
  };

  const handleRowClick = (item) => {
    if (!props.rowClickable) {
      return;
    }
    emit('row-click', item);
  };

  const changePage = (nextPage) => {
    if (nextPage < 0 || nextPage > totalPages.value - 1) {
      return;
    }
    emit('page-change', nextPage);
  };
</script>

<style scoped>
  .table td,
  .table th {
    padding-top: 1rem;
    padding-bottom: 1rem;
  }

  .table-row-hover {
    transition: all 0.2s ease-in-out;
  }

  .table-row-hover:hover {
    background-color: #f8f9fa;
  }

  .cursor-pointer {
    cursor: pointer;
  }
</style>
