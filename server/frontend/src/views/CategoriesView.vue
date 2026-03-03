<template>
  <div class="container-fluid px-2 px-md-3 py-3 py-md-4">
    <div class="row">
      <div class="col-12">
        <!-- Page Header -->
        <PageHeader
          title="Categories"
          mobile-title="Categories"
          subtitle="Manage quality check categories"
          icon="bi bi-tags"
        >
          <template #actions>
            <button
              @click="refreshCategories"
              class="btn btn-outline-primary btn-sm"
              :disabled="loading"
            >
              <i class="bi bi-arrow-clockwise"></i>
              <span class="d-none d-md-inline ms-1">Refresh</span>
            </button>
            <button
              @click="createCategory"
              class="btn btn-primary btn-sm ms-2"
              :disabled="loading"
            >
              <i class="bi bi-plus-lg"></i>
              <span class="d-none d-md-inline ms-1">New Category</span>
            </button>
          </template>
        </PageHeader>

        <!-- Stats Cards -->
        <div class="stats-grid mb-3 mb-md-4">
          <div class="stat-card">
            <div class="stat-number text-dark">{{ categories.length }}</div>
            <div class="stat-label">Total Categories</div>
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
                placeholder="Search categories..."
              >
            </div>
            <div class="results-count">
              <span class="text-muted small">{{ filteredCategories.length }} categories</span>
            </div>
          </div>
        </div>

        <!-- Loading state -->
        <div v-if="loading" class="loading-state">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading categories...</span>
          </div>
        </div>

        <!-- Error state -->
        <div v-else-if="error" class="alert alert-danger" role="alert">
          <h6 class="alert-heading">Error Loading Categories</h6>
          <p class="mb-0">{{ error }}</p>
        </div>

        <!-- Empty state -->
        <div v-else-if="filteredCategories.length === 0" class="empty-state">
          <div class="empty-state-icon">
            <i class="bi bi-tags"></i>
          </div>
          <h5 class="empty-state-title">No Categories Found</h5>
          <p class="empty-state-text">
            {{ searchQuery ? 'Try adjusting your search criteria' : 'No categories are configured yet' }}
          </p>
        </div>

        <!-- Categories Table -->
        <div v-else class="card border-0 shadow-sm">
          <div class="card-header bg-white border-bottom py-3">
            <div class="d-flex justify-content-between align-items-center">
              <h5 class="mb-0 fw-semibold">Category Definitions</h5>
              <span class="badge bg-secondary">{{ filteredCategories.length }} categories</span>
            </div>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-hover mb-0 align-middle">
                <thead class="table-light">
                  <tr>
                    <th class="ps-4">Name</th>
                    <th>Color</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="category in filteredCategories"
                    :key="category.id"
                    class="table-row-hover cursor-pointer"
                    @click="viewCategoryDetail(category)"
                  >
                    <td class="ps-4">
                      <div class="fw-medium">{{ category.name }}</div>
                    </td>
                    <td>
                      <CategoryBadge :category="category" />
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { apiService } from '../services/apiService.js'
import PageHeader from '../components/PageHeader.vue'
import CategoryBadge from '../components/CategoryBadge.vue'

const router = useRouter()
const categories = ref([])
const loading = ref(false)
const error = ref(null)
const searchQuery = ref('')

const filteredCategories = computed(() => {
  if (!searchQuery.value) {
    return categories.value
  }

  const query = searchQuery.value.toLowerCase()
  return categories.value.filter(category =>
    category.name?.toLowerCase().includes(query)
  )
})

const loadCategories = async () => {
  loading.value = true
  error.value = null

  try {
    const data = await apiService.getCategories()
    // Handle HAL format response
    if (data._embedded && data._embedded.categories) {
      categories.value = data._embedded.categories
    } else if (Array.isArray(data)) {
      categories.value = data
    } else {
      categories.value = []
    }
  } catch (err) {
    error.value = err.message || 'Failed to load categories'
    console.error('Error loading categories:', err)
  } finally {
    loading.value = false
  }
}

const refreshCategories = () => {
  loadCategories()
}

const createCategory = () => {
  router.push('/categories/new')
}

const viewCategoryDetail = (category) => {
  router.push(`/categories/${category.id}`)
}

onMounted(() => {
  loadCategories()
})
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

.table-row-hover {
  transition: all 0.2s ease-in-out;
  cursor: pointer;
}

.table-row-hover:hover {
  background-color: #f8f9fa;
  transform: translateX(2px);
  box-shadow: inset 3px 0 0 #0d6efd;
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
