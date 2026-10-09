<template>
  <div class="audit-log-page">
    <PageHeader
      title="Audit Log"
      mobile-title="Audit Log"
      subtitle="Review who changed what and when on this agent"
      icon="bi bi-journal-text"
    >
      <template #actions>
        <ActionButton
          :loading="exporting"
          icon="bi bi-download"
          text="Export CSV"
          @click="exportCsv"
        />
      </template>
    </PageHeader>

    <div class="page-content">
      <!-- Filters -->
      <div class="card filters-card mb-4 border-0 shadow-sm">
        <div class="card-header bg-white">
          <div class="d-flex align-items-center gap-2">
            <i class="bi bi-funnel text-primary fs-5"></i>
            <h5 class="mb-0">Filters</h5>
          </div>
        </div>
        <div class="card-body">
          <div class="filters-row">
            <div class="search-bar">
              <i class="bi bi-search search-icon"></i>
              <input
                v-model="filters.search"
                type="text"
                class="form-control filter-control"
                placeholder="Search actor, action or details..."
                aria-label="Search audit log"
              />
              <button
                v-if="filters.search"
                class="btn btn-link clear-btn"
                type="button"
                aria-label="Clear search"
                @click="filters.search = ''"
              >
                <i class="bi bi-x-circle"></i>
              </button>
            </div>
            <select
              v-model="filters.action"
              class="form-select filter-control action-select"
              aria-label="Filter by action"
            >
              <option value="">All Actions</option>
              <option v-for="action in actions" :key="action" :value="action">
                {{ formatAuditAction(action) }}
              </option>
            </select>
            <div class="filter-item date-filter">
              <label for="auditDateFrom" class="filter-label">From</label>
              <input
                id="auditDateFrom"
                v-model="filters.dateFrom"
                type="date"
                class="form-control filter-control"
                :max="filters.dateTo || undefined"
              />
            </div>
            <div class="filter-item date-filter">
              <label for="auditDateTo" class="filter-label">To</label>
              <input
                id="auditDateTo"
                v-model="filters.dateTo"
                type="date"
                class="form-control filter-control"
                :min="filters.dateFrom || undefined"
              />
            </div>
            <div class="filter-item filter-button-group">
              <button
                class="btn filter-control clear-filters-btn"
                :disabled="!hasActiveFilters"
                @click="clearFilters"
              >
                <i class="bi bi-x-circle"></i>
                Clear
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>

      <BaseTable
        title="Audit Entries"
        :loading="loading"
        :columns="columns"
        :items="entries"
        :total-elements="pagination.totalElements"
        :total-pages="pagination.totalPages"
        :current-page="pagination.number"
        item-label="entries"
        :empty-text="hasActiveFilters ? 'No entries match the filters' : 'No audit entries yet'"
        empty-icon="bi bi-journal-x"
        @page-change="handlePageChange"
      >
        <template #header-timestamp>
          Timestamp
          <span class="total-count ms-1">({{ pagination.totalElements }} total)</span>
        </template>
        <template #timestamp="{ value }">
          <span class="text-nowrap">{{ formatDateShort(value) }} {{ formatTime(value) }}</span>
        </template>
        <template #action="{ value }">
          <span class="badge" :class="getAuditActionBadgeClass(value)">
            {{ formatAuditAction(value) }}
          </span>
        </template>
        <template #module="{ value }">
          {{ value || '—' }}
        </template>
        <template #entityId="{ value }">
          {{ value == null ? '—' : `#${value}` }}
        </template>
        <template #details="{ value }">
          <span :title="value">{{ value || '—' }}</span>
        </template>
      </BaseTable>
    </div>
  </div>
</template>

<script setup>
  import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import PageHeader from '@/components/PageHeader.vue';
  import BaseTable from '@/components/BaseTable.vue';
  import ActionButton from '@/components/ActionButton.vue';
  import { auditService } from '@/services/auditService.js';
  import { notificationService } from '@/services/notificationService.js';
  import { formatAuditAction, getAuditActionBadgeClass } from '@/utils/auditUtils.js';
  import { formatDateShort, formatTime } from '@/utils/stringUtils.js';
  import { downloadBlob } from '@/utils/downloadUtils.js';

  const PAGE_SIZE = 20;
  const SEARCH_DEBOUNCE_MS = 300;

  const route = useRoute();
  const router = useRouter();

  const entries = ref([]);
  const pagination = ref({ number: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 });
  const loading = ref(true);
  const error = ref('');
  const exporting = ref(false);
  const actions = ref([]);
  const filters = reactive({ search: '', action: '', dateFrom: '', dateTo: '' });

  let latestRequestId = 0;
  let searchTimer = null;

  const columns = [
    { key: 'timestamp', label: 'Timestamp' },
    { key: 'actor', label: 'Actor' },
    { key: 'action', label: 'Action' },
    { key: 'module', label: 'Module', headerClass: 'hide-md', cellClass: 'hide-md' },
    {
      key: 'entityId',
      label: 'Entity',
      headerClass: 'center hide-sm',
      cellClass: 'center hide-sm',
    },
    { key: 'details', label: 'Details', headerClass: 'hide-lg', cellClass: 'truncate hide-lg' },
  ];

  const hasActiveFilters = computed(
    () => !!(filters.search || filters.action || filters.dateFrom || filters.dateTo)
  );

  const queryFilters = computed(() => ({
    search: filters.search.trim(),
    action: filters.action,
    dateFrom: filters.dateFrom ? `${filters.dateFrom}T00:00:00` : '',
    dateTo: filters.dateTo ? `${filters.dateTo}T23:59:59.999999999` : '',
  }));

  const getPageFromUrl = () => {
    const page = parseInt(route.query.page, 10);
    return isNaN(page) || page < 0 ? 0 : page;
  };

  const fetchEntries = async () => {
    const requestId = ++latestRequestId;
    loading.value = true;
    error.value = '';
    try {
      const result = await auditService.getAll({
        page: getPageFromUrl(),
        size: PAGE_SIZE,
        ...queryFilters.value,
      });
      // Ignore responses from requests superseded by a newer filter or page change
      if (requestId !== latestRequestId) return;
      entries.value = result.items;
      pagination.value = result.page;
    } catch (err) {
      if (requestId !== latestRequestId) return;
      console.error('Failed to load audit log:', err);
      error.value = 'Unable to load the audit log. Please try again.';
      notificationService.error('Load Failed', 'Unable to load the audit log.');
      entries.value = [];
    } finally {
      if (requestId === latestRequestId) {
        loading.value = false;
      }
    }
  };

  const fetchActions = async () => {
    try {
      actions.value = await auditService.getActions();
    } catch (err) {
      console.error('Failed to load audit actions:', err);
    }
  };

  const exportCsv = async () => {
    exporting.value = true;
    try {
      const { blob, filename } = await auditService.exportCsv(queryFilters.value);
      downloadBlob(blob, filename);
    } catch (err) {
      console.error('Failed to export audit log:', err);
      notificationService.error('Export Failed', 'Unable to export the audit log.');
    } finally {
      exporting.value = false;
    }
  };

  const handlePageChange = (page) => {
    router.replace({ query: { ...route.query, page: page.toString() } });
  };

  const applyFilters = () => {
    if (getPageFromUrl() !== 0) {
      handlePageChange(0);
    } else {
      fetchEntries();
    }
  };

  const clearFilters = () => {
    Object.assign(filters, { search: '', action: '', dateFrom: '', dateTo: '' });
  };

  watch(() => route.query.page, fetchEntries);

  watch(() => [filters.action, filters.dateFrom, filters.dateTo], applyFilters);

  watch(
    () => filters.search,
    () => {
      clearTimeout(searchTimer);
      searchTimer = setTimeout(applyFilters, SEARCH_DEBOUNCE_MS);
    }
  );

  onMounted(() => {
    fetchEntries();
    fetchActions();
  });

  onBeforeUnmount(() => clearTimeout(searchTimer));
</script>

<style scoped>
  .audit-log-page {
    min-height: 100%;
    padding: 2rem;
  }

  .page-content {
    width: 100%;
  }

  .filters-card {
    border-radius: var(--radius-lg);
  }

  .filters-card .card-header {
    border-bottom: 1px solid var(--color-gray-100);
  }

  .filters-row {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    gap: var(--spacing-md);
  }

  .search-bar {
    position: relative;
    flex: 1 1 280px;
    max-width: 400px;
  }

  .action-select {
    flex: 0 1 240px;
  }

  .date-filter {
    flex: 0 1 170px;
  }

  .search-bar .form-control {
    padding-left: 2.5rem;
    padding-right: 2.5rem;
  }

  .filter-control {
    border: 2px solid var(--color-gray-200);
    border-radius: var(--radius-md);
    background-color: var(--bg-card);
  }

  .filter-control:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 4px color-mix(in srgb, var(--color-primary) 10%, transparent);
  }

  .clear-filters-btn {
    display: inline-flex;
    align-items: center;
    gap: var(--spacing-xs);
    color: var(--color-gray-600);
  }

  .clear-filters-btn:hover:not(:disabled) {
    border-color: var(--color-gray-300);
    background-color: var(--color-gray-100);
    color: var(--color-gray-800);
  }

  .search-icon {
    position: absolute;
    left: 0.875rem;
    top: 50%;
    transform: translateY(-50%);
    color: var(--color-gray-400);
    font-size: 1rem;
    pointer-events: none;
  }

  .clear-btn {
    position: absolute;
    right: 0.25rem;
    top: 50%;
    transform: translateY(-50%);
    color: var(--color-gray-400);
    padding: 0.25rem 0.5rem;
    text-decoration: none;
  }

  .clear-btn:hover {
    color: var(--color-gray-600);
  }

  .filter-item {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-xs);
  }

  .filter-label {
    font-size: 0.75rem;
    font-weight: 600;
    color: var(--color-gray-500);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .filter-button-group {
    flex-direction: row;
    justify-content: flex-start;
  }

  .total-count {
    font-weight: 500;
    text-transform: none;
    letter-spacing: normal;
    color: var(--color-gray-400);
  }

  .badge {
    font-size: 0.75rem;
    font-weight: 600;
    padding: 0.35rem 0.65rem;
  }

  @media (max-width: 768px) {
    .audit-log-page {
      padding: 1rem;
    }

    .search-bar,
    .action-select {
      flex-basis: 100%;
      max-width: 100%;
    }

    .date-filter {
      flex: 1 1 0;
    }
  }

  @media (max-width: 576px) {
    .audit-log-page {
      padding: 0.75rem;
    }
  }
</style>
