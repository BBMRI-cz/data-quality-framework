<template>
  <div class="container-fluid px-2 px-md-3 py-3 py-md-4">
    <div class="row">
      <div class="col-12">
        <!-- Page Header -->
        <PageHeader
          title="Groups"
          mobile-title="Groups"
          subtitle="Manage agent groups"
          icon="bi bi-collection-fill"
        >
          <template #actions>
            <button
              @click="refreshGroups"
              class="btn btn-outline-primary btn-sm"
              :disabled="loading"
            >
              <i class="bi bi-arrow-clockwise"></i>
              <span class="d-none d-md-inline ms-1">Refresh</span>
            </button>
            <button
              @click="createGroup"
              class="btn btn-primary btn-sm ms-2"
              :disabled="loading"
            >
              <i class="bi bi-plus-lg"></i>
              <span class="d-none d-md-inline ms-1">New Group</span>
            </button>
          </template>
        </PageHeader>

        <!-- Stats Cards -->
        <div class="stats-grid mb-3 mb-md-4">
          <div class="stat-card">
            <div class="stat-number text-dark">{{ groups.length }}</div>
            <div class="stat-label">Total Groups</div>
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
                placeholder="Search groups..."
              >
            </div>
            <div class="results-count">
              <span class="text-muted small">{{ filteredGroups.length }} groups</span>
            </div>
          </div>
        </div>

        <!-- Loading state -->
        <div v-if="loading" class="loading-state">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Loading groups...</span>
          </div>
        </div>

        <!-- Error state -->
        <div v-else-if="error" class="alert alert-danger" role="alert">
          <h6 class="alert-heading">Error Loading Groups</h6>
          <p class="mb-0">{{ error }}</p>
        </div>

        <!-- Empty state -->
        <div v-else-if="filteredGroups.length === 0" class="empty-state">
          <div class="empty-state-icon">
            <i class="bi bi-collection"></i>
          </div>
          <h5 class="empty-state-title">No Groups Found</h5>
          <p class="empty-state-text">
            {{ searchQuery ? 'Try adjusting your search criteria' : 'No groups are configured yet' }}
          </p>
        </div>

        <!-- Groups Table -->
        <div v-else class="card border-0 shadow-sm">
          <div class="card-header bg-white border-bottom py-3">
            <div class="d-flex justify-content-between align-items-center">
              <h5 class="mb-0 fw-semibold">Group Definitions</h5>
              <span class="badge bg-secondary">{{ filteredGroups.length }} groups</span>
            </div>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-hover mb-0 align-middle">
                <thead class="table-light">
                  <tr>
                    <th class="ps-4">Name</th>
                    <th>Agents</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="group in filteredGroups"
                    :key="group.id"
                    class="table-row-hover cursor-pointer"
                    @click="viewGroupDetail(group)"
                  >
                    <td class="ps-4">
                      <div class="fw-medium">{{ group.name }}</div>
                    </td>
                    <td>
                      <span class="badge bg-primary">
                        {{ group.agentIds?.length || 0 }} agents
                      </span>
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

const router = useRouter()
const groups = ref([])
const loading = ref(false)
const error = ref(null)
const searchQuery = ref('')

const filteredGroups = computed(() => {
  if (!searchQuery.value) {
    return groups.value
  }

  const query = searchQuery.value.toLowerCase()
  return groups.value.filter(group =>
    group.name?.toLowerCase().includes(query)
  )
})

const loadGroups = async () => {
  loading.value = true
  error.value = null

  try {
    const data = await apiService.getGroups()
    // Handle HAL format response
    if (data._embedded && data._embedded.groups) {
      groups.value = data._embedded.groups
    } else if (Array.isArray(data)) {
      groups.value = data
    } else {
      groups.value = []
    }
  } catch (err) {
    error.value = err.message || 'Failed to load groups'
    console.error('Error loading groups:', err)
  } finally {
    loading.value = false
  }
}

const refreshGroups = () => {
  loadGroups()
}

const createGroup = () => {
  router.push('/groups/new')
}

const viewGroupDetail = (group) => {
  router.push(`/groups/${group.id}`)
}

onMounted(() => {
  loadGroups()
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
  line-height: 1;
  margin-bottom: 0.5rem;
}

.stat-label {
  color: #6c757d;
  font-size: 0.875rem;
  font-weight: 500;
}

/* Filters Card */
.filters-card {
  background: white;
  padding: 1.25rem;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.filters-content {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}

.search-filter {
  flex: 1;
  min-width: 200px;
}

.results-count {
  display: flex;
  align-items: center;
}

/* States */
.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 3rem;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.empty-state-icon {
  font-size: 4rem;
  color: #dee2e6;
  margin-bottom: 1rem;
}

.empty-state-title {
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.empty-state-text {
  color: #6c757d;
  margin-bottom: 0;
}

/* Table */
.table-row-hover {
  transition: all 0.2s ease;
}

.table-row-hover:hover {
  background-color: rgba(102, 126, 234, 0.05);
  transform: translateX(2px);
}

.cursor-pointer {
  cursor: pointer;
}

@media (max-width: 576px) {
  .stat-number {
    font-size: 1.5rem;
  }

  .stat-label {
    font-size: 0.75rem;
  }
}
</style>

