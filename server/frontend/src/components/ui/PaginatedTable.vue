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

    <div v-if="loading" class="card-body table-state text-center" :style="bodyStyle">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">{{ loadingText }}</span>
      </div>
    </div>

    <div v-else-if="error" class="card-body table-state" :style="bodyStyle">
      <div class="alert alert-danger mb-0" role="alert">
        <h6 class="alert-heading">{{ errorTitle }}</h6>
        <p class="mb-0">{{ error }}</p>
      </div>
    </div>

    <div v-else-if="totalItems === 0" class="card-body table-state text-center" :style="bodyStyle">
      <h5 class="mb-2">{{ emptyTitle }}</h5>
      <p class="text-muted mb-0">{{ emptyText }}</p>
    </div>

    <div v-else ref="contentRef" class="card-body p-0" :style="bodyStyle">
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
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="showPagination && !loading && !error" class="card-footer bg-white border-top py-2">
      <div class="d-flex justify-content-end align-items-center gap-2">
        <button
          type="button"
          class="btn btn-sm btn-outline-secondary"
          :disabled="page === 0"
          aria-label="Previous page"
          @click="changePage(page - 1, $event)"
        >
          Previous
        </button>
        <span class="small text-muted">Page {{ page + 1 }} of {{ totalPages }}</span>
        <button
          type="button"
          class="btn btn-sm btn-outline-secondary"
          :disabled="page >= totalPages - 1"
          aria-label="Next page"
          @click="changePage(page + 1, $event)"
        >
          Next
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { computed, nextTick, onMounted, ref, watch } from 'vue';

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
      default: 'No items found',
    },
    emptyTitle: {
      type: String,
      default: 'No Results Found',
    },
    loading: {
      type: Boolean,
      default: false,
    },
    loadingText: {
      type: String,
      default: 'Loading...',
    },
    error: {
      type: String,
      default: '',
    },
    errorTitle: {
      type: String,
      default: 'Unable to load data',
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

    const shouldSlice =
      typeof props.totalItems !== 'number' || props.totalItems === props.items.length;
    if (!shouldSlice) {
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

  const pendingRestore = ref(null);
  const contentRef = ref(null);
  const contentMinHeight = ref(null);

  const updateContentHeight = () => {
    if (!contentRef.value) {
      return;
    }
    const height = contentRef.value.getBoundingClientRect().height;
    if (height > 0) {
      contentMinHeight.value = height;
    }
  };

  const bodyStyle = computed(() => {
    if (!contentMinHeight.value) {
      return undefined;
    }
    return { minHeight: `${contentMinHeight.value}px` };
  });

  const restoreScrollAndFocus = async () => {
    const pending = pendingRestore.value;
    if (!pending) {
      return;
    }
    pendingRestore.value = null;

    await nextTick();

    const scrollingElement = document.scrollingElement || document.documentElement;
    const restoreScrollPosition = () => {
      if (scrollingElement && typeof pending.scrollTop === 'number') {
        scrollingElement.scrollTop = pending.scrollTop;
      }
    };
    restoreScrollPosition();
    requestAnimationFrame(() => {
      restoreScrollPosition();
    });

    if (pending.focusedElement?.focus) {
      try {
        pending.focusedElement.focus({ preventScroll: true });
      } catch (error) {
        pending.focusedElement.focus();
      }
    }
  };

  watch(
    () => props.loading,
    (isLoading) => {
      if (!isLoading) {
        restoreScrollAndFocus();
        nextTick(updateContentHeight);
      }
    }
  );

  watch(paginatedItems, () => {
    if (!props.loading) {
      nextTick(updateContentHeight);
    }
  });

  onMounted(() => {
    nextTick(updateContentHeight);
  });

  const changePage = (nextPage, event) => {
    if (nextPage < 0 || nextPage > totalPages.value - 1) {
      return;
    }
    const focusedElement = event?.currentTarget || null;
    const scrollingElement = document.scrollingElement || document.documentElement;
    const scrollTop = scrollingElement ? scrollingElement.scrollTop : null;
    emit('page-change', nextPage);

    pendingRestore.value = { scrollTop, focusedElement };
    if (!props.loading) {
      restoreScrollAndFocus();
    }
  };
</script>

<style scoped>
  .table-state {
    padding: 3rem 1rem;
  }

  .table td,
  .table th {
    padding-top: 1rem;
    padding-bottom: 1rem;
  }

  .table-responsive {
    overflow-anchor: none;
  }

  .table-row-hover {
    transition: all 0.2s ease-in-out;
  }

  .table-row-hover:hover {
    background-color: var(--bg-hover);
  }

  .cursor-pointer {
    cursor: pointer;
  }
</style>
