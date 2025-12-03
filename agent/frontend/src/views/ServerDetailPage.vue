<template>
  <div class="server-detail-page">
    <PageHeader
      :title="`Server: ${server?.name || ''}`"
      :mobileTitle="`Server: ${server?.name || ''}`"
      subtitle="Detailed view of server configuration and interactions"
      icon="bi bi-server"
    />

    <div class="page-content">
      <!-- Back button -->
      <div class="mb-3">
        <button class="btn btn-outline-secondary btn-sm" @click="goBack">
          <i class="bi bi-arrow-left me-2"></i>Back to Servers
        </button>
      </div>

      <!-- Loading state -->
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status" style="width: 3rem; height: 3rem;">
          <span class="visually-hidden">Loading...</span>
        </div>
        <p class="text-muted mt-3">Loading server details...</p>
      </div>

      <!-- Error state -->
      <div v-else-if="error" class="alert alert-danger">
        <i class="bi bi-exclamation-triangle me-2"></i>
        {{ error }}
      </div>

      <!-- Server details -->
      <template v-else-if="server">
        <!-- Server Info Card -->
        <div class="card border-0 shadow-sm mb-4">
          <div class="card-header bg-white border-bottom">
            <div class="d-flex justify-content-between align-items-center">
              <h5 class="mb-0">Server Information</h5>
              <span :class="['badge', 'rounded-pill', getStatusBadgeClass(server.status)]">
                <i :class="getStatusIcon(server.status)" class="me-1"></i>
                {{ formatStatus(server.status) }}
              </span>
            </div>
          </div>
          <div class="card-body">
            <div class="row g-4">
              <div class="col-md-6">
                <div class="info-group">
                  <label class="info-label">
                    <i class="bi bi-tag me-2"></i>Server Name
                  </label>
                  <p class="info-value">{{ server.name }}</p>
                </div>
              </div>
              <div class="col-md-6">
                <div class="info-group">
                  <label class="info-label">
                    <i class="bi bi-link-45deg me-2"></i>Server URL
                  </label>
                  <p class="info-value">
                    <a :href="server.url" target="_blank" rel="noopener noreferrer" class="server-link">
                      {{ server.url }}
                      <i class="bi bi-box-arrow-up-right ms-1"></i>
                    </a>
                  </p>
                </div>
              </div>
              <div class="col-md-6">
                <div class="info-group">
                  <label class="info-label">
                    <i class="bi bi-key me-2"></i>Client ID
                  </label>
                  <p class="info-value font-monospace">{{ server.clientId || 'N/A' }}</p>
                </div>
              </div>
              <div class="col-md-6">
                <div class="info-group">
                  <label class="info-label">
                    <i class="bi bi-clock-history me-2"></i>Total Interactions
                  </label>
                  <p class="info-value">{{ server.interactions?.length || 0 }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Interactions Table -->
        <div class="card border-0 shadow-sm">
          <div class="card-header bg-white border-bottom py-3">
            <div class="d-flex justify-content-between align-items-center flex-wrap gap-3">
              <h5 class="mb-0 fw-semibold">Interaction History</h5>
              <span class="badge bg-secondary">{{ filteredInteractions.length }} interactions</span>
            </div>

            <!-- Filters -->
            <div class="row g-3 mt-2">
              <div class="col-md-4">
                <label class="form-label small text-muted">Filter by Type</label>
                <select v-model="filterType" class="form-select form-select-sm">
                  <option value="">All Types</option>
                  <option value="UPDATE">Update</option>
                  <option value="COMMUNICATION">Communication</option>
                  <option value="REGISTRATION">Registration</option>
                </select>
              </div>
              <div class="col-md-4">
                <label class="form-label small text-muted">Search Description</label>
                <input
                  v-model="searchQuery"
                  type="text"
                  class="form-control form-control-sm"
                  placeholder="Search in descriptions..."
                />
              </div>
              <div class="col-md-4">
                <label class="form-label small text-muted">&nbsp;</label>
                <button
                  @click="clearFilters"
                  class="btn btn-outline-secondary btn-sm w-100"
                  :disabled="!filterType && !searchQuery"
                >
                  <i class="bi bi-x-circle me-1"></i>Clear Filters
                </button>
              </div>
            </div>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-hover mb-0 align-middle">
                <thead class="table-light">
                  <tr>
                    <th class="ps-4">Timestamp</th>
                    <th>Type</th>
                    <th>Description</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="interaction in paginatedInteractions" :key="interaction.id">
                    <td class="ps-4">
                      <div class="d-flex flex-column">
                        <span class="fw-medium">{{ formatDateShort(interaction.timestamp) }}</span>
                        <small class="text-muted">{{ formatTime(interaction.timestamp) }}</small>
                      </div>
                    </td>
                    <td>
                      <span class="badge" :class="getInteractionTypeBadge(interaction.type)">
                        {{ interaction.type }}
                      </span>
                    </td>
                    <td>
                      <div class="d-flex align-items-center gap-2">
                        <span class="text-muted">{{ truncateDescription(interaction.description) }}</span>
                        <button
                          v-if="isValidJson(interaction.description)"
                          @click="openJsonModal(interaction.description)"
                          class="btn btn-sm btn-outline-primary"
                          title="View JSON details"
                        >
                          <i class="bi bi-braces me-1"></i>View JSON
                        </button>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="filteredInteractions.length === 0">
                    <td colspan="3" class="text-center text-muted py-5">
                      <i class="bi bi-inbox fs-1 d-block mb-2 opacity-50"></i>
                      <p class="mb-0">
                        {{ (filterType || searchQuery) ? 'No interactions match your filters' : 'No interactions logged yet' }}
                      </p>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="card-footer bg-white border-top">
            <div class="d-flex justify-content-between align-items-center">
              <div class="text-muted small">
                Showing {{ startEntry }} to {{ endEntry }} of {{ filteredInteractions.length }} interactions
              </div>
              <Pagination
                :current-page="currentPage"
                :total-pages="totalPages"
                :page-size="pageSize"
                :max-visible-buttons="5"
                @page-changed="onPageChanged"
              />
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Delete Confirmation Modal -->
    <DeleteConfirmModal
      v-if="showDeleteModal"
      :item-name="server?.name || 'this server'"
      :loading="isDeleting"
      @close="closeDeleteModal"
      @confirm="confirmDelete"
    />

    <!-- JSON Viewer Modal -->
    <div v-if="showJsonModal" class="modal d-block" style="background-color: rgba(0, 0, 0, 0.5);">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Interaction Details</h5>
            <button
              type="button"
              class="btn-close"
              @click="closeJsonModal"
              aria-label="Close"
            ></button>
          </div>
          <div class="modal-body">
            <div class="json-viewer">
              <pre>{{ formattedJson }}</pre>
            </div>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              class="btn btn-secondary"
              @click="closeJsonModal"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '@/js/api.js';
import PageHeader from '@/components/PageHeader.vue';
import Pagination from '@/components/Pagination.vue';
import DeleteConfirmModal from '@/components/DeleteConfirmModal.vue';
import {
  getStatusBadgeClass,
  getStatusIcon,
  formatStatus
} from '@/utils/serverStatus.js';
import {
  getInteractionTypeBadge
} from '@/utils/interactionTypes.js';
import { notificationService } from '@/services/notificationService.js';

const route = useRoute();
const router = useRouter();

const server = ref(null);
const loading = ref(true);
const error = ref(null);

// Delete modal state
const showDeleteModal = ref(false);
const isDeleting = ref(false);

// Pagination state
const currentPage = ref(1);
const pageSize = 50;

// Filter state
const filterType = ref('');
const searchQuery = ref('');

// JSON Modal state
const showJsonModal = ref(false);
const formattedJson = ref('');

const sortedInteractions = computed(() => {
  if (!server.value?.interactions) return [];
  return [...server.value.interactions].sort((a, b) =>
    new Date(b.timestamp) - new Date(a.timestamp)
  );
});

const filteredInteractions = computed(() => {
  let interactions = sortedInteractions.value;

  // Filter by type
  if (filterType.value) {
    interactions = interactions.filter(i => i.type === filterType.value);
  }

  // Filter by search query
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    interactions = interactions.filter(i =>
      i.description.toLowerCase().includes(query)
    );
  }

  return interactions;
});

const totalPages = computed(() => {
  return Math.ceil(filteredInteractions.value.length / pageSize);
});

const paginatedInteractions = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  const end = start + pageSize;
  return filteredInteractions.value.slice(start, end);
});

const startEntry = computed(() => {
  if (filteredInteractions.value.length === 0) return 0;
  return (currentPage.value - 1) * pageSize + 1;
});

const endEntry = computed(() => {
  const end = currentPage.value * pageSize;
  return Math.min(end, filteredInteractions.value.length);
});

function onPageChanged(page) {
  currentPage.value = page;
}

function clearFilters() {
  filterType.value = '';
  searchQuery.value = '';
  currentPage.value = 1;
}

// Reset to page 1 when filters change
function resetPagination() {
  currentPage.value = 1;
}

// Watch for filter changes
watch([filterType, searchQuery], () => {
  resetPagination();
});

async function fetchServerDetails() {
  loading.value = true;
  error.value = null;

  try {
    const response = await api.get(`/api/servers/${route.params.id}`);
    server.value = response.data;
  } catch (err) {
    console.error('Error fetching server details:', err);
    error.value = err.response?.data?.message || 'Failed to load server details';
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push('/servers');
}

function formatDateShort(dateString) {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  });
}

function formatTime(dateString) {
  const date = new Date(dateString);
  return date.toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

function isValidJson(str) {
  try {
    JSON.parse(str);
    return true;
  } catch (e) {
    return false;
  }
}

function truncateDescription(description, maxLength = 100) {
  if (!description) return '';
  if (description.length <= maxLength) return description;
  return description.substring(0, maxLength) + '...';
}

function openJsonModal(jsonString) {
  try {
    const jsonObject = JSON.parse(jsonString);
    formattedJson.value = JSON.stringify(jsonObject, null, 2);
    showJsonModal.value = true;
  } catch (e) {
    console.error('Failed to parse JSON:', e);
  }
}

function closeJsonModal() {
  showJsonModal.value = false;
  formattedJson.value = '';
}

function handleDelete() {
  showDeleteModal.value = true;
}

function closeDeleteModal() {
  showDeleteModal.value = false;
}

async function confirmDelete() {
  if (!server.value) return;

  isDeleting.value = true;
  try {
    await api.delete(`/api/servers/${server.value.id}`);
    notificationService.success('Server Deleted', `${server.value.name} has been deleted successfully`);
    router.push('/servers');
  } catch (err) {
    console.error('Error deleting server:', err);
    notificationService.error('Delete Failed', err.response?.data?.message || 'Unable to delete server. Please try again.');
  } finally {
    isDeleting.value = false;
    showDeleteModal.value = false;
  }
}

onMounted(() => {
  fetchServerDetails();
});
</script>

<style scoped>
.server-detail-page {
  min-height: 100%;
  padding: var(--spacing-xl);
}

.page-content {
  width: 100%;
}

.info-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.info-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-gray-600);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  display: flex;
  align-items: center;
  margin-bottom: 0;
}

.info-value {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--color-gray-800);
  margin-bottom: 0;
  word-break: break-word;
}

.server-link {
  color: var(--color-primary);
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  transition: color var(--transition-base);
}

.server-link:hover {
  color: var(--color-primary-dark);
  text-decoration: underline;
}

.btn-outline-secondary {
  border-color: var(--color-gray-300);
  color: var(--color-gray-700);
}

.btn-outline-secondary:hover {
  background-color: var(--color-gray-100);
  border-color: var(--color-gray-400);
  color: var(--color-gray-900);
}

@media (max-width: 768px) {
  .page-content {
    padding: var(--spacing-md);
  }
}

@media (max-width: 576px) {
  .page-content {
    padding: var(--spacing-sm);
  }
}

.json-viewer {
  background-color: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  padding: 1rem;
  overflow-x: auto;
  max-height: 600px;
  overflow-y: auto;
}

.json-viewer pre {
  margin: 0;
  color: #333;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.875rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
