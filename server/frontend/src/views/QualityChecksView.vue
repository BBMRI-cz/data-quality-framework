<template>
  <div class="container-fluid px-2 px-md-3 py-3 py-md-4">
    <div class="row">
      <div class="col-12">
        <!-- Page Header -->
        <PageHeader
          title="Quality Checks"
          mobile-title="Checks"
          subtitle="View and manage data quality check definitions"
          icon="bi bi-check-square"
        >
          <template #actions>
            <button
              class="btn btn-outline-primary btn-sm"
              :disabled="loading"
              @click="refreshChecks"
            >
              <i class="bi bi-arrow-clockwise"></i>
              <span class="d-none d-md-inline ms-1">Refresh</span>
            </button>
          </template>
        </PageHeader>

        <!-- Stats Cards -->
        <div class="stats-grid mb-3 mb-md-4">
          <StatsCard label="Total Checks" :value="qualityChecks.length" :animation-duration="600" />
        </div>

        <!-- Filters -->
        <div class="filters-card mb-3 mb-md-4">
          <div class="filters-content">
            <div class="search-filter">
              <input
                v-model="searchQuery"
                type="text"
                class="form-control"
                placeholder="Search quality checks..."
              />
            </div>
            <div class="category-filter-container">
              <ValuesFilter v-model="selectedCategory" :categories="categories" />
            </div>
            <div class="results-count">
              <span class="text-muted small">{{ filteredChecks.length }} checks</span>
            </div>
          </div>
        </div>

        <!-- Loading state -->
        <div v-if="loading" class="loading-state">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading quality checks...</span>
          </div>
        </div>

        <!-- Error state -->
        <div v-else-if="error" class="alert alert-danger" role="alert">
          <h6 class="alert-heading">Error Loading Quality Checks</h6>
          <p class="mb-0">{{ error }}</p>
        </div>

        <!-- Empty state -->
        <div v-else-if="filteredChecks.length === 0" class="empty-state">
          <div class="empty-state-icon">
            <i class="bi bi-clipboard-check"></i>
          </div>
          <h5 class="empty-state-title">No Quality Checks Found</h5>
          <p class="empty-state-text">
            {{
              searchQuery
                ? 'Try adjusting your search criteria'
                : 'No quality checks are configured yet'
            }}
          </p>
        </div>

        <!-- Quality Checks Table -->
        <PaginatedTable
          v-else
          title="Quality Check Definitions"
          :columns="tableColumns"
          :items="tableRows"
          :page="currentPage"
          :page-size="pageSize"
          :total-items="tableRows.length"
          item-key="hash"
          item-label="checks"
          empty-text="No quality checks are configured yet"
          @row-click="viewCheckDetail"
          @page-change="onPageChange"
        >
          <template #header-meta>
            <Badge :text="`${filteredChecks.length} checks`" variant="secondary" size="small" />
          </template>

          <template #cell-category="{ item }">
            <ValuesFilterBadge :category="item.category" />
          </template>
        </PaginatedTable>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted, watch } from 'vue';
  import { apiService } from '@/services/apiService.js';
  import Badge from '@/components/ui/Badge.vue';
  import StatsCard from '@/components/ui/StatsCard.vue';
  import PaginatedTable from '@/components/ui/PaginatedTable.vue';
  import PageHeader from '@/components/ui/PageHeader.vue';
  import ValuesFilter from '@/components/ui/ValuesFilter.vue';
  import ValuesFilterBadge from '@/components/ui/ValuesFilterBadge.vue';
  import { useRouter } from 'vue-router';
  import { formatDateShort } from '@/utils/dateUtils.js';

  const router = useRouter();

  const qualityChecks = ref([]);
  const loading = ref(false);
  const error = ref(null);
  const searchQuery = ref('');
  const selectedCategory = ref(null);
  const currentPage = ref(0);
  const pageSize = 10;

  const tableColumns = [
    {
      key: 'hash',
      label: 'Hash',
      headerClass: 'ps-4 d-none d-lg-table-cell',
      cellClass: 'ps-4 d-none d-lg-table-cell',
    },
    { key: 'name', label: 'Name' },
    { key: 'category', label: 'Category' },
    {
      key: 'description',
      label: 'Description',
      headerClass: 'd-none d-md-table-cell',
      cellClass: 'd-none d-md-table-cell',
      fallback: 'No description',
    },
    {
      key: 'registeredAtText',
      label: 'Registered At',
      headerClass: 'd-none d-xl-table-cell',
      cellClass: 'd-none d-xl-table-cell',
      fallback: 'N/A',
    },
  ];

  const categories = computed(() => {
    const cats = new Set();
    qualityChecks.value.forEach((check) => {
      if (check.category && check.category.name) {
        cats.add(check.category.name);
      } else {
        cats.add('No Category');
      }
    });
    return Array.from(cats).sort();
  });

  const filteredChecks = computed(() => {
    let checks = qualityChecks.value;

    if (selectedCategory.value) {
      checks = checks.filter((check) => {
        const categoryName = check.category?.name || 'No Category';
        return categoryName === selectedCategory.value;
      });
    }

    if (!searchQuery.value) {
      return checks;
    }

    const query = searchQuery.value.toLowerCase();
    return checks.filter(
      (check) =>
        check.name?.toLowerCase().includes(query) ||
        check.description?.toLowerCase().includes(query) ||
        check.hash?.toLowerCase().includes(query) ||
        check.category?.name?.toLowerCase().includes(query)
    );
  });

  const tableRows = computed(() => {
    return filteredChecks.value.map((check) => ({
      ...check,
      registeredAtText: check.registeredAt ? formatDateShort(check.registeredAt) : null,
    }));
  });

  const onPageChange = (page) => {
    currentPage.value = page;
  };

  const loadQualityChecks = async () => {
    loading.value = true;
    error.value = null;

    try {
      const data = await apiService.getQualityChecks();
      // Handle HAL format response
      if (data._embedded && data._embedded.qualityChecks) {
        qualityChecks.value = data._embedded.qualityChecks;
      } else if (Array.isArray(data)) {
        qualityChecks.value = data;
      } else {
        qualityChecks.value = [];
      }
    } catch (err) {
      error.value = err.message || 'Failed to load quality checks';
      console.error('Error loading quality checks:', err);
    } finally {
      loading.value = false;
    }
  };

  const refreshChecks = () => {
    loadQualityChecks();
  };

  const viewCheckDetail = (check) => {
    router.push(`/quality-checks/${check.hash}`);
  };

  watch([searchQuery, selectedCategory], () => {
    currentPage.value = 0;
  });

  watch(tableRows, (rows) => {
    const maxPage = Math.max(Math.ceil(rows.length / pageSize) - 1, 0);
    if (currentPage.value > maxPage) {
      currentPage.value = maxPage;
    }
  });

  onMounted(() => {
    loadQualityChecks();
  });
</script>

<style scoped>
  /* Stats Grid */
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 1rem;
  }

  /* Filters */
  .filters-card {
    background: white;
    padding: 1rem;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  .filters-content {
    display: flex;
    flex-wrap: wrap;
    gap: 1rem;
    align-items: center;
  }

  .search-filter {
    flex: 1;
    min-width: 200px;
  }

  .results-count {
    margin-left: auto;
  }

  /* Loading State */
  .loading-state {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 4rem 0;
  }

  /* Empty State */
  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  .empty-state-icon {
    font-size: 4rem;
    color: #e0e0e0;
    margin-bottom: 1rem;
  }

  .empty-state-title {
    color: #2c3e50;
    font-weight: 600;
    margin-bottom: 0.5rem;
  }

  .empty-state-text {
    color: #6c757d;
    margin-bottom: 0;
  }

  /* Responsive */
  @media (max-width: 768px) {
    .stats-grid {
      grid-template-columns: repeat(3, 1fr);
    }

    .filters-content {
      flex-direction: column;
      align-items: stretch;
    }

    .results-count {
      margin-left: 0;
      text-align: center;
    }
  }

  @media (max-width: 576px) {
    .stats-grid {
      grid-template-columns: repeat(3, 1fr);
      gap: 0.5rem;
    }

    .container-fluid {
      padding-left: 0.75rem;
      padding-right: 0.75rem;
    }
  }
</style>
