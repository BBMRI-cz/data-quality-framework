<template>
  <div class="quality-checks-page">
    <PageHeader
      title="Quality Checks"
      mobile-title="Checks"
      subtitle="View and manage Data Quality Checks for your data"
      icon="bi bi-check-square"
    />

    <div class="page-content">
      <div class="page-actions">
        <ActionButton to="/quality-checks/new" icon="bi bi-plus" text="Add Quality Check" />
      </div>

      <div class="stats-grid">
        <StatCard
          :number="totalChecks"
          label="Total Checks"
          number-class="text-dark"
          help-text="Total number of quality checks configured"
        />
      </div>

      <div class="filter-section">
        <label class="filter-label">
          <i class="bi bi-funnel"></i>
          Filter by Category
        </label>
        <CategoryFilter v-model="selectedCategoryName" :categories="categories" />
      </div>

      <QualityCheckTable
        :items="qualityChecks"
        :loading="loading"
        :error="error"
        :pagination="pagination"
        @page-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import QualityCheckTable from '@/components/QualityCheckTable.vue';
  import PageHeader from '@/components/PageHeader.vue';
  import StatCard from '@/components/StatCard.vue';
  import ActionButton from '@/components/ActionButton.vue';
  import CategoryFilter from '@/components/CategoryFilter.vue';
  import { useQualityChecks } from '@/composables/useQualityChecks.js';
  import { categoryService } from '@/services/categoryService.js';
  import { notificationService } from '@/services/notificationService.js';

  const route = useRoute();
  const router = useRouter();
  const { qualityChecks, loading, error, pagination, totalChecks, fetchChecks } =
    useQualityChecks();
  const categories = ref([]);
  const selectedCategoryName = ref(null);

  const categoryNameForApi = computed(() => {
    if (selectedCategoryName.value === 'none') {
      return '';
    }
    return selectedCategoryName.value;
  });

  const getPageFromUrl = () => {
    const pageParam = route.query.page;
    const page = parseInt(pageParam, 10);
    return isNaN(page) || page < 0 ? 0 : page;
  };

  const loadChecks = () => {
    fetchChecks({
      page: getPageFromUrl(),
      size: pagination.value.size,
      categoryName: categoryNameForApi.value,
    });
  };

  const handlePageChange = (page) => {
    router.replace({ query: { ...route.query, page: page.toString() } });
  };

  const loadCategories = async () => {
    try {
      categories.value = await categoryService.getAll();
    } catch (error) {
      console.error('Failed to load categories:', error);
      notificationService.error('Load Failed', 'Unable to load categories. Please try again.');
    }
  };

  const filterParams = computed(() => ({
    page: getPageFromUrl(),
    categoryName: categoryNameForApi.value,
  }));

  watch(
    filterParams,
    () => {
      loadChecks();
    },
    { deep: true }
  );

  watch(selectedCategoryName, () => {
    router.replace({ query: { ...route.query, page: '0' } });
  });

  onMounted(() => {
    loadCategories();
    loadChecks();
  });
</script>

<style scoped>
  .quality-checks-page {
    min-height: 100%;
    padding: var(--spacing-xl);
  }

  .page-content {
    width: 100%;
  }

  .page-actions {
    display: flex;
    justify-content: flex-end;
    margin-bottom: var(--spacing-md);
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: var(--spacing-md);
    margin-bottom: var(--spacing-lg);
  }

  .filter-section {
    margin-bottom: var(--spacing-lg);
    padding: var(--spacing-md);
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
  }

  .filter-label {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--color-gray-600);
    margin-bottom: var(--spacing-sm);
  }

  .filter-label i {
    color: var(--color-primary);
  }

  @media (max-width: 768px) {
    .quality-checks-page {
      padding: var(--spacing-md);
    }
  }

  @media (max-width: 576px) {
    .quality-checks-page {
      padding: var(--spacing-sm);
    }

    .stats-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
