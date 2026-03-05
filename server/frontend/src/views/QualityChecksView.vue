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
          <div class="stat-card">
            <div class="stat-number text-dark">{{ qualityChecks.length }}</div>
            <div class="stat-label">Total Checks</div>
          </div>
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
              <CategoryFilter v-model="selectedCategory" :categories="categories" />
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
        <div v-else class="card border-0 shadow-sm">
          <div class="card-header bg-white border-bottom py-3">
            <div class="d-flex justify-content-between align-items-center">
              <h5 class="mb-0 fw-semibold">Quality Check Definitions</h5>
              <span class="badge bg-secondary">{{ filteredChecks.length }} checks</span>
            </div>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-hover mb-0 align-middle">
                <thead class="table-light">
                  <tr>
                    <th class="ps-4 d-none d-lg-table-cell">Hash</th>
                    <th>Name</th>
                    <th>Category</th>
                    <th class="d-none d-md-table-cell">Description</th>
                    <th class="d-none d-xl-table-cell">Registered At</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="check in filteredChecks"
                    :key="check.hash || check.name"
                    class="table-row-hover cursor-pointer"
                    @click="viewCheckDetail(check)"
                  >
                    <td class="ps-4 d-none d-lg-table-cell">
                      <code class="font-monospace small text-muted hash-code">{{
                        check.hash
                      }}</code>
                    </td>
                    <td>
                      <div class="fw-medium">{{ check.name }}</div>
                      <div class="d-md-none small text-muted mt-1">{{ check.description }}</div>
                    </td>
                    <td>
                      <CategoryBadge :category="check.category" />
                    </td>
                    <td class="d-none d-md-table-cell">
                      <div class="text-muted small">
                        {{ check.description || 'No description' }}
                      </div>
                    </td>
                    <td class="d-none d-xl-table-cell">
                      <span class="text-muted small">{{ formatDate(check.registeredAt) }}</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue';
  import { apiService } from '../services/apiService.js';
  import PageHeader from '../components/PageHeader.vue';
  import CategoryFilter from '../components/CategoryFilter.vue';
  import CategoryBadge from '../components/CategoryBadge.vue';
  import { useRouter } from 'vue-router';

  const router = useRouter();

  const qualityChecks = ref([]);
  const loading = ref(false);
  const error = ref(null);
  const searchQuery = ref('');
  const selectedCategory = ref(null);

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

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const options = { year: 'numeric', month: '2-digit', day: '2-digit' };
    return new Date(dateString).toLocaleDateString(undefined, options);
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

  .stat-card {
    background: white;
    padding: 1.25rem;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    text-align: center;
  }

  .stat-number {
    font-size: 2rem;
    font-weight: 700;
    margin-bottom: 0.25rem;
  }

  .stat-label {
    font-size: 0.875rem;
    color: #6c757d;
    font-weight: 500;
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

  /* Table Styling */
  .card {
    border-radius: 12px;
    overflow: hidden;
  }

  .table {
    font-size: 0.875rem;
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
  }

  .table-responsive {
    overflow-x: visible;
  }

  .hash-code {
    max-width: 150px;
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

  .cursor-pointer {
    cursor: pointer;
  }

  .font-monospace {
    font-family: var(--font-mono), monospace;
    font-size: 0.875rem;
  }

  .badge {
    font-weight: 500;
    padding: 0.35rem 0.65rem;
    font-size: 0.75rem;
    white-space: nowrap;
  }

  /* Responsive */
  @media (max-width: 992px) {
    .table th,
    .table td {
      padding: 0.75rem 0.5rem;
    }

    .hash-code {
      max-width: 120px;
    }
  }

  @media (max-width: 768px) {
    .stats-grid {
      grid-template-columns: repeat(3, 1fr);
    }

    .stat-number {
      font-size: 1.5rem;
    }

    .stat-label {
      font-size: 0.75rem;
    }

    .filters-content {
      flex-direction: column;
      align-items: stretch;
    }

    .results-count {
      margin-left: 0;
      text-align: center;
    }

    .table {
      font-size: 0.75rem;
    }

    .table th,
    .table td {
      padding: 0.5rem 0.35rem;
    }

    .hash-code {
      max-width: 100px;
    }

    .badge {
      font-size: 0.65rem;
      padding: 0.25rem 0.45rem;
    }
  }

  @media (max-width: 576px) {
    .stats-grid {
      grid-template-columns: repeat(3, 1fr);
      gap: 0.5rem;
    }

    .stat-card {
      padding: 0.875rem 0.5rem;
    }

    .container-fluid {
      padding-left: 0.75rem;
      padding-right: 0.75rem;
    }

    .table-responsive {
      overflow-x: auto;
    }
  }
</style>
