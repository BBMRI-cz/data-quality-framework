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
          <div class="filters-top-row">
            <div class="search-filter">
              <input
                v-model="searchQuery"
                type="text"
                class="form-control"
                placeholder="Search quality checks..."
              />
            </div>
            <div class="results-count">
              <span class="text-muted small">{{ filteredChecks.length }} checks</span>
            </div>
          </div>
        </div>
        <div class="category-filter-container mt-3">
          <LabeledValuesFilter
              v-model="selectedCategory"
              label="Categories:"
              :categories="categories"
          />
        </div>

        <!-- Quality Checks Table -->
        <PaginatedTable
          title="Quality Check Definitions"
          :columns="tableColumns"
          :items="tableRows"
          :page="currentPage"
          :page-size="pageSize"
          :total-items="tableRows.length"
          :loading="loading"
          :error="error"
          :empty-text="emptyStateText"
          item-key="hash"
          item-label="checks"
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
  import LabeledValuesFilter from '@/components/ui/LabeledValuesFilter.vue';
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
    { key: 'hash', label: 'Hash' },
    { key: 'name', label: 'Name' },
    { key: 'category', label: 'Category' },
    { key: 'description', label: 'Description' },
    {
      key: 'registeredAtText',
      label: 'Registered At',
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
      description: check.description || 'No description',
      registeredAtText: check.registeredAt ? formatDateShort(check.registeredAt) : null,
    }));
  });

  const emptyStateText = computed(() => {
    return searchQuery.value
      ? 'Try adjusting your search criteria'
      : 'No quality checks are configured yet';
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

  .filters-top-row {
    display: flex;
    gap: 1rem;
    align-items: center;
  }

  .search-filter {
    flex: 1;
    min-width: 200px;
  }

  .category-filter-container {
    min-width: 240px;
  }

  .results-count {
    margin-left: auto;
  }


  /* Responsive */
  @media (max-width: 768px) {
    .stats-grid {
      grid-template-columns: repeat(3, 1fr);
    }

    .results-count {
      margin-left: 0;
      text-align: left;
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
