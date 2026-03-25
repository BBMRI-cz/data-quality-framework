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
              <SearchBar v-model="searchQuery" placeholder="Search quality checks..." />
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
  import { onMounted } from 'vue';
  import Badge from '@/components/ui/Badge.vue';
  import StatsCard from '@/components/ui/StatsCard.vue';
  import PaginatedTable from '@/components/ui/PaginatedTable.vue';
  import PageHeader from '@/components/ui/PageHeader.vue';
  import LabeledValuesFilter from '@/components/ui/LabeledValuesFilter.vue';
  import SearchBar from '@/components/ui/SearchBar.vue';
  import ValuesFilterBadge from '@/components/ui/ValuesFilterBadge.vue';
  import { useQualityChecksTable } from '@/composables/useQualityChecksTable.js';
  import { useRouter } from 'vue-router';

  const router = useRouter();

  const {
    qualityChecks,
    loading,
    error,
    searchQuery,
    selectedCategory,
    currentPage,
    pageSize,
    categories,
    filteredChecks,
    tableRows,
    emptyStateText,
    onPageChange,
    loadQualityChecks,
    refreshChecks,
  } = useQualityChecksTable();

  const tableColumns = [
    { key: 'hash', label: 'Hash' },
    { key: 'name', label: 'Name' },
    { key: 'category', label: 'Category' },
    { key: 'description', label: 'Description' },
    { key: 'registeredAtText', label: 'Registered At' },
  ];

  const viewCheckDetail = (check) => {
    router.push(`/quality-checks/${check.hash}`);
  };

  onMounted(loadQualityChecks);
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

  /* Responsive */
  @media (max-width: 768px) {
    .stats-grid {
      grid-template-columns: repeat(3, 1fr);
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
