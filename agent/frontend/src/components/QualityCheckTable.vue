<template>
  <div>

    <!-- Loading state -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
    </div>

    <!-- Error state -->
    <div v-else-if="error" class="error-state">
      <h6>Error Loading Quality Checks</h6>
      <p>{{ error }}</p>
    </div>

    <!-- Quality Checks Table -->
    <BaseTable
      v-else
      title="Quality Checks"
      :columns="columns"
      :items="qualityChecks"
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
      <template #query="{ value }">
        {{ truncateText(value, 30) }}
      </template>
    </BaseTable>
  </div>
</template>

<script setup>
import { onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import BaseTable from '@/components/BaseTable.vue';
import { useQualityChecks } from '@/composables/useQualityChecks.js';
import { truncateText } from '@/utils/stringUtils.js';

const router = useRouter();
const route = useRoute();
const { qualityChecks, loading, error, pagination, fetchChecks } = useQualityChecks();

const columns = [
  { key: 'name', label: 'Name' },
  { key: 'description', label: 'Description', headerClass: 'hide-md', cellClass: 'hide-md', fallback: 'No description' },
  { key: 'query', label: 'Query', headerClass: 'hide-lg', cellClass: 'hide-lg truncate' },
  { key: 'warningThreshold', label: 'Warning', headerClass: 'center hide-lg', cellClass: 'center warning hide-lg' },
  { key: 'errorThreshold', label: 'Error', headerClass: 'center hide-lg', cellClass: 'center danger hide-lg' },
  { key: 'epsilonBudget', label: 'Epsilon', headerClass: 'center hide-xl', cellClass: 'center hide-xl', format: 'decimal' },
];

const navigateToEdit = (item) => {
  router.push(`/quality-checks/${item.id}/edit`);
};

const handlePageChange = (page) => {
  router.replace({ query: { ...route.query, page: page.toString() } });
};

const getPageFromUrl = () => {
  const pageParam = route.query.page;
  const page = parseInt(pageParam, 10);
  return isNaN(page) || page < 0 ? 0 : page;
};

watch(
  () => route.query.page,
  () => {
    fetchChecks({ page: getPageFromUrl(), size: pagination.value.size });
  }
);

onMounted(() => {
  fetchChecks({ page: getPageFromUrl(), size: pagination.value.size });
});
</script>

<style scoped>

.icon {
  color: var(--color-primary);
  margin-right: var(--spacing-sm);
}

.loading-state {
  display: flex;
  justify-content: center;
  padding: 4rem 0;
}

.spinner {
  width: 2rem;
  height: 2rem;
  border: 3px solid var(--color-gray-200);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
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
