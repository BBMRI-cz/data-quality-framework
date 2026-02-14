<template>
  <div>
    <!-- Filters and Actions -->
    <div class="filters-card">
      <div class="filters-content">
        <input
          v-model="searchQuery"
          type="text"
          class="search-input"
          placeholder="Search quality checks..."
        />
        <span class="results-count">{{ filteredChecks.length }} checks</span>
        <router-link to="/quality-checks/new" class="add-button">
          <i class="bi bi-plus"></i> Add Check
        </router-link>
      </div>
    </div>

    <!-- Loading state -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
    </div>

    <!-- Error state -->
    <div v-else-if="error" class="error-state">
      <h6>Error Loading Quality Checks</h6>
      <p>{{ error }}</p>
    </div>

    <!-- Empty search state -->
    <div v-else-if="filteredChecks.length === 0 && searchQuery" class="empty-state">
      <i class="bi bi-search"></i>
      <h5>No Results Found</h5>
      <p>Try adjusting your search criteria</p>
    </div>

    <!-- Quality Checks Table -->
    <BaseTable
      v-else
      title="Quality Checks"
      :columns="columns"
      :items="filteredChecks"
      :item-count="filteredChecks.length"
      item-label="checks"
      empty-text="No quality checks configured yet"
      empty-icon="bi bi-clipboard-check"
      @row-click="navigateToEdit"
    >
      <template #name="{ item }">
        <i class="bi bi-check2-square icon"></i>
        {{ item.name }}
      </template>
      <template #query="{ value }">
        {{ truncateText(value, 30) }}
      </template>
    </BaseTable>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import BaseTable from '@/components/BaseTable.vue';
import { useQualityChecks } from '@/composables/useQualityChecks.js';
import { truncateText } from '@/utils/stringUtils.js';

const router = useRouter();
const { filteredChecks, loading, error, searchQuery, fetchChecks } = useQualityChecks();

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

onMounted(fetchChecks);
</script>

<style scoped>
.filters-card {
  background: var(--bg-card);
  padding: var(--spacing-md);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: var(--spacing-lg);
}

.filters-content {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.search-input {
  flex: 1;
  min-width: 200px;
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  font-size: 0.875rem;
}

.search-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.results-count {
  color: var(--color-gray-500);
  font-size: 0.875rem;
}

.add-button {
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--color-success);
  color: white;
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-weight: 500;
  text-decoration: none;
}

.add-button:hover {
  opacity: 0.9;
}

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
