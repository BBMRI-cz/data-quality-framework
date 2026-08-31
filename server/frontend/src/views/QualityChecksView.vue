<template>
  <div class="container-fluid py-3 py-md-4">
    <PageHeader
      title="Quality Checks"
      mobile-title="Checks"
      subtitle="View and manage data quality check definitions"
      icon="bi bi-check-square"
    >
      <template #actions>
        <button class="btn btn-outline-primary btn-sm" :disabled="loading" @click="refreshChecks">
          <i class="bi bi-arrow-clockwise"></i>
          <span class="d-none d-md-inline ms-1">Refresh</span>
        </button>
      </template>
    </PageHeader>

    <div class="row g-3 mb-3 mb-md-4">
      <div class="col-12 col-sm-6 col-lg-4">
        <StatsCard
          label="Total Checks"
          :value="qualityChecks.length"
          :animation-duration="600"
          icon="bi bi-check-square"
          color="var(--color-primary)"
        />
      </div>
    </div>

    <div class="mb-3 mb-md-4">
      <SearchBar v-model="searchQuery" placeholder="Search quality checks..." />
    </div>

    <div class="mb-3 mb-md-4">
      <LabeledValuesFilter
        v-model="selectedCategory"
        label="Categories:"
        :categories="categories"
      />
    </div>

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
    { key: 'id', label: 'ID' },
    { key: 'hash', label: 'Hash' },
    { key: 'name', label: 'Name' },
    { key: 'category', label: 'Category' },
    { key: 'description', label: 'Description' },
    { key: 'registeredAtText', label: 'Registered At' },
  ];

  const viewCheckDetail = (check) => {
    router.push(`/quality-checks/${check.id}`);
  };

  onMounted(loadQualityChecks);
</script>
