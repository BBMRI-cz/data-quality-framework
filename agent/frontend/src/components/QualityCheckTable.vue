<template>
  <div>
    <!-- Filters and Actions -->
    <div class="filters-card mb-3 mb-md-4">
      <div class="filters-content">
        <div class="search-filter">
          <input
            v-model="searchQuery"
            type="text"
            class="form-control"
            placeholder="Search quality checks..."
          >
        </div>
        <div class="results-count">
          <span class="text-muted small">{{ filteredChecks.length }} checks</span>
        </div>
        <router-link to="/quality-checks/new" class="btn btn-success">
          <i class="bi bi-plus me-1"></i>Add Check
        </router-link>
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
        {{ searchQuery ? 'Try adjusting your search criteria' : 'No quality checks are configured yet' }}
      </p>
    </div>

    <!-- Quality Checks Table -->
    <div v-else class="card border-0 shadow-sm">
      <div class="card-header bg-white border-bottom py-3">
        <div class="d-flex justify-content-between align-items-center">
          <h5 class="mb-0 fw-semibold">Quality Checks</h5>
          <span class="badge bg-secondary">{{ filteredChecks.length }} checks</span>
        </div>
      </div>
      <div class="card-body p-0">
        <div>
          <table class="table table-hover mb-0 align-middle">
            <thead class="table-light">
              <tr>
                <th class="ps-4">Name</th>
                <th class="d-none d-md-table-cell">Description</th>
                <th class="d-none d-lg-table-cell">Query</th>
                <th class="text-center d-none d-lg-table-cell">Warning</th>
                <th class="text-center d-none d-lg-table-cell">Error</th>
                <th class="text-center d-none d-xl-table-cell">Epsilon Budget</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="check in filteredChecks"
                :key="check.id"
                class="table-row-hover"
                @click="navigateToEdit(check.id)"
              >
                <td class="ps-4">
                  <div class="d-flex align-items-center">
                    <i class="bi bi-check2-square text-primary me-2"></i>
                    <span class="fw-medium">{{ check.name }}</span>
                  </div>
                  <div class="d-md-none small text-muted mt-1">{{ check.description }}</div>
                </td>
                <td class="d-none d-md-table-cell">
                  <span class="text-muted small">{{ check.description || 'No description' }}</span>
                </td>
                <td class="d-none d-lg-table-cell">
                  <code class="font-monospace small text-muted query-code">{{ truncateQuery(check.query, 30) }}</code>
                </td>
                <td class="text-center d-none d-lg-table-cell">
                  <span class="badge bg-warning-subtle text-warning-emphasis">
                    {{ check.warningThreshold }}
                  </span>
                </td>
                <td class="text-center d-none d-lg-table-cell">
                  <span class="badge bg-danger-subtle text-danger-emphasis">
                    {{ check.errorThreshold }}
                  </span>
                </td>
                <td class="text-center d-none d-xl-table-cell">
                  <span class="text-muted">{{ check.epsilonBudget.toFixed(2) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { api } from "../js/api.js";

const router = useRouter();
const qualityChecks = ref([]);
const loading = ref(false);
const error = ref(null);
const searchQuery = ref('');

const url = '/api/quality-checks';

const filteredChecks = computed(() => {
  if (!searchQuery.value) {
    return qualityChecks.value;
  }

  const query = searchQuery.value.toLowerCase();
  return qualityChecks.value.filter(check =>
    check.name?.toLowerCase().includes(query) ||
    check.description?.toLowerCase().includes(query) ||
    check.query?.toLowerCase().includes(query)
  );
});

const fetchChecks = async () => {
  loading.value = true;
  error.value = null;

  try {
    const { data } = await api.get(url);
    qualityChecks.value = data._embedded?.qualityChecks || [];
  } catch (err) {
    error.value = err.message || 'Failed to load quality checks';
    console.error('Error fetching checks:', err);
    qualityChecks.value = [];
  } finally {
    loading.value = false;
  }
};

const navigateToEdit = (id) => {
  router.push(`/quality-checks/${id}/edit`);
};

const truncateQuery = (query, maxLength = 50) => {
  if (!query) return '';
  return query.length > maxLength ? `${query.slice(0, maxLength)}...` : query;
};

onMounted(fetchChecks);
</script>

<style scoped>
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

/* Table Styling */
.card {
  border-radius: 12px;
  overflow: hidden;
}

.table {
  font-size: 0.875rem;
  table-layout: fixed;
  width: 100%;
}

.table th {
  font-weight: 600;
  font-size: 0.813rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #6c757d;
  padding: 1rem 0.75rem;
  border-bottom: 2px solid #dee2e6;
  white-space: nowrap;
}

.table td {
  vertical-align: middle;
  padding: 1rem 0.75rem;
  border-bottom: 1px solid #f0f0f0;
  font-size: 0.875rem;
  word-wrap: break-word;
}

.query-code {
  max-width: 100%;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-row-hover {
  transition: all 0.2s ease-in-out;
  cursor: pointer;
}

.table-row-hover:hover {
  background-color: #f8f9fa;
  transform: translateX(2px);
  box-shadow: inset 3px 0 0 #0d6efd;
}

.font-monospace {
  font-family: 'SF Mono', 'Monaco', 'Courier New', monospace;
  font-size: 0.875rem;
}

.badge {
  font-weight: 500;
  padding: 0.35rem 0.65rem;
  font-size: 0.75rem;
  white-space: nowrap;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .filters-content {
    flex-direction: column;
    align-items: stretch;
  }

  .results-count {
    margin-left: 0;
  }
}
</style>